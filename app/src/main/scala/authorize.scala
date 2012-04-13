package giter8

import dispatch._
import dispatch.liftjson.Js._
import net.liftweb.json.JsonAST._

object Authorize {
  val authorizations = :/("api.github.com").secure / "authorizations"
}

trait Authorize { self: Giter8 =>
  import Authorize._
  
  def auth(user: String, pass: String): Either[String, String] =
    http(authorizations.POST.as_!(user, pass) ># { _ \ "token" match {
      case JString(tok) => Some(tok)
      case _ => None
    }}).map { access =>
      Config.properties { 
        _.setProperty("gh.access", access)
      }
      Right("Authorization stored")
    } getOrElse Left("Authorization failed")
}
