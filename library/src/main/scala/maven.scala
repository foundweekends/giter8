package giter8

import java.io.File

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

  private def mavenRepo(): Option[String] = {
    val home = Option(System.getProperty("G8_HOME")).map(new File(_)).getOrElse(
      new File(System.getProperty("user.home"), ".g8")
    )
    val mvnRepo = new File(home + "/mvnrepo")
    if(!home.exists() || !mvnRepo.exists()) None
    else {
      scala.io.Source.fromFile(mvnRepo).getLines().filter(!_.trim.isEmpty).find(_=>true)
    }
  }

  private def latestVersion(
    org: String,
    name: String
  ): Either[String, String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val mvnCentral: String = "https://repo1.maven.org/maven2"
    val loc = mavenRepo().getOrElse(mvnCentral) + s"/${org.replace('.', '/')}/$name/maven-metadata.xml"
    val fut = for (resp <- G8.http(dispatch.url(loc))) yield {
      resp.getStatusCode match {
        case 200 =>
          (dispatch.as.xml.Elem(resp) \ "versioning" \ "latest").headOption.map(
            elem => elem.text
          ).toRight(s"Found metadata at $loc but can't extract latest version")
        case 404 =>
          Left(s"Maven metadata not found for `maven($org, $name)`\nTried: $loc")
        case status =>
          Left(s"Unexpected response status $status fetching metadata from $loc")
      }
    }
    Await.result(fut, 1.minute)
  }

  def lookup(rawDefaults: G8.OrderedProperties): Either[String, G8.OrderedProperties] = {
    val defaults = rawDefaults.map {
      case (k, Maven(org, name)) => k -> latestVersion(org, name)
      case (k, value)            => k -> Right(value)
    }
    val initial: Either[String, G8.OrderedProperties] = Right(List.empty)
    defaults.reverseIterator.foldLeft(initial) {
      case (accumEither, (k, either)) =>
        for {
          cur   <- accumEither.right
          value <- either.right
        } yield (k -> value) :: cur
    }
  }
}
