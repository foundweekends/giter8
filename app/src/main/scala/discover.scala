package giter8

trait Discover { self: Giter8 =>
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._

  val RepoNamed = """(\S+)\.g8""".r
  def discover(query: Option[String]) =
    remote_templates(query).right.flatMap { templates =>
      templates match {
        case Nil => Right("No templates matching %s" format query.get)
        case _ => Right(templates.sortBy { _.name } map { t =>
          "%-40s%s" format(t.user + "/" + t.name, t.desc)
        } mkString("\n"))
      }
    }
  
  def remote_templates(query: Option[String]) = try { Right(for {
    repos <- http(repoSearch(query) ># ('repositories ? ary))
    JObject(fields) <- repos
    JField("name", JString(repo)) <- fields
    JField("username", JString(user_name)) <- fields
    JField("description", JString(desc)) <- fields
    repo_name <- RepoNamed.findFirstMatchIn(repo)
  } yield Template(user_name, repo_name.group(1), desc)) } catch {
    case StatusCode(404, _) => Left("Unable to find github repositories like : %s" format query.get)
  }

  case class Template(user: String, name: String, desc: String)
 
  def repoSearch(query: Option[String]) = gh / "repos" / "search" / query.getOrElse("g8")
}
