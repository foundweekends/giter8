package giter8
import dispatch.classic._
import dispatch.classic.json._

case class GithubRepo(name: String,
                      description: String)

object GithubRepo {
  def apply(data: JsValue): Option[GithubRepo] = data match {
    case o: JsObject => for {
      JsString(name) <- o.self.get(JsString("full_name"))
      JsString(description) <- o.self.get(JsString("description"))
    } yield GithubRepo(name, description)
    case _ => None
  }


  def parseJson(data:String): Option[JsValue] =
    try {
      Some(JsValue.fromString(data))
    } catch {
      case e: Exception => None
    }

  private val searchUrl =
    "https://api.github.com/search/repositories"

  def search(pattern: String): List[GithubRepo] = {
    val query = "%s .g8" format pattern

    val handler = url(searchUrl) <:< Map(
      "User-Agent" -> "Giter8"
    ) <<? Seq(
      ("q" , query)
    ) >- { responseStr =>
      for {
        JsObject(response) <- parseJson(responseStr)
        JsArray(items) <- response.get(JsString("items"))
        repos = items.map(GithubRepo(_)).flatten
      } yield repos
    }

    Http(handler).getOrElse(Nil)
  }
}

