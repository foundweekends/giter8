package giter8

import scala.util.parsing.combinator._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Parse any ls.implcit.ly references in default properties. The latest
 * version may be requested by setting a property's value to
 * 
 *   ls(library, user, repo)
 *
 * The second two parameters are optional.
 */
object Ls extends JavaTokenParsers {
  def spec =
    "ls" ~> "(" ~> word ~ optElem ~ optElem <~ ")" ^^ {
      case library ~ user ~ repo => (library, user, repo)
    }
  def optElem = opt("," ~> word)
  /** Like ident but allow hyphens */
  def word = """[\w\-]+""".r

  def unapply(value: String) =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }

  def lookup(rawDefaults: Map[String,String])
  : Either[String, Map[String,String]] = {
    val lsDefaults = rawDefaults.view.collect {
      case (key, Ls(library, user, repo)) =>
        ls.DefaultClient {
          _.Handler.latest(library, user, repo)
        }.right.map { future =>
          Await.result(future, 1.minute).right.map(key -> _)
        }.joinRight
    }
    val initial: Either[String,Map[String,String]] = Right(rawDefaults)
    (initial /: lsDefaults) { (accumEither, lsEither) =>
      for {
        cur <- accumEither.right
        ls <- lsEither.right
      } yield cur + ls
    }.left.map { "Error retrieving ls version info: " + _ }
  }
}
