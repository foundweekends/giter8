package giter8

import dispatch.host
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.Exception.allCatch
import scala.util.parsing.combinator._

/**
 * Parse `maven-metadata.xml`
 */
object Maven extends JavaTokenParsers {
  private val org, name = """[\w\-\.]+""".r

  private val spec =
    "maven" ~> "(" ~> org ~ ("," ~> name) <~ ")" ^^ {
      case org ~ name =>
        (org, name)
    }

  private def unapply(value: String): Option[(String, String)] =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }

  private def latestVersion(org: String, name: String): Either[String, String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val errorMessage = s"""could not find latest version of "$org % $name" """
    allCatch.either {
      val url = dispatch.url(s"https://repo1.maven.org/maven2/${org.replace('.', '/')}/$name/maven-metadata.xml")
      ls.DefaultClient.http(url OK dispatch.as.xml.Elem).map { xml =>
        (xml \ "versioning" \ "latest").headOption.map(_.text).toRight(errorMessage)
      }
    } match {
      case Right(future) =>
        Await.result(future, 1.minute)
      case Left(error) =>
        error.printStackTrace()
        Left(errorMessage + " " + error)
    }
  }

  def lookup(rawDefaults: G8.OrderedProperties): Either[String, G8.OrderedProperties] = {
    val defaults = rawDefaults.collect {
      case (key, Maven(org, name)) =>
        latestVersion(org, name).right.map(key -> _)
    }
    val initial: Either[String, G8.OrderedProperties] = Right(rawDefaults)
    defaults.foldLeft(initial) { (accumEither, either) =>
      for {
        cur <- accumEither.right
        version <- either.right
      } yield {
        val (inits, tail) = cur.span { case (k, _) => k != version._1 }
        inits ++ (version +: (tail.tail))
      }
    }.left.map { "Error retrieving maven-metadata.xml version info: " + _ }
  }
}
