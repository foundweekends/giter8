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

  private def latestVersion(
    org: String,
    name: String
  ): Either[String, String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val loc = s"https://repo1.maven.org/maven2/${org.replace('.', '/')}/$name/maven-metadata.xml"
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
    defaults.foldLeft(initial) {
      case (accumEither, (k, either)) =>
        for {
          cur   <- accumEither.right
          value <- either.right
        } yield (k -> value) :: cur
    }
  }
}
