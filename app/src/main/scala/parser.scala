package giter8

import scala.util.parsing.combinator._

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
    "ls" ~> "(" ~> ident ~ optElem ~ optElem <~ ")" ^^ {
      case library ~ user ~ repo => (library, user, repo)
    }
  def optElem = opt("," ~> ident)

  def unapply(value: String) =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }
}
