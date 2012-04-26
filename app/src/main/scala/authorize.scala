package giter8

import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object Authorize {
  val authorizations = :/("api.github.com").secure / "authorizations"
}

trait Authorize { self: Giter8 =>
  import Authorize._
  
  def auth(user: String, pass: String): Either[String, String] =
    http x (authorizations.POST.as_!(user, pass) << compact(render(
          ("note" -> "Giter8") ~
          ("note_url" -> "https://github.com/n8han/giter8")
    )) ># { _ \ "token" match {
      case JString(tok) => Some(tok)
      case _ => None
    }}) {
      case (201, _, _, token) => token().map { access =>
        Config.properties {
          _.setProperty("gh.access", access)
        }
        Right("Authorization stored")
      } getOrElse Left("Authorization failed")
      case (401, _, _, _) =>
        Left("Invalid github.com login/password combination")
      case (status, _, _, _) =>
        Left("Unexpected error communicating with api.github.com")
    }
}
