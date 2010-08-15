package giter8

class Giter8 extends xsbti.AppMain {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.collection.JavaConversions._
  import java.io.{File,FileWriter}

  val re = "^src/main/g8/(.+)$".r
  val Param = """^--(\S+)=(.+)$""".r
  val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
  
  def run(config: xsbti.AppConfiguration) =
    config.arguments.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Repo(user, proj))) => copy("%s/%s.g8".format(user, proj), params)
      case _ =>
        println("\nUsage: g8 <gituser/project.g8> [--param=value ...]")
        new Exit(0)
    }

  def copy(repo: String, params: Iterable[String]) = {
    val repo_files = for {
      blobs <- http(gh / "blob" / "all" / repo / "master" ># ('blobs ? obj))
      JField(name, JString(hash)) <- blobs
      m <- re.findFirstMatchIn(name)
    } yield (m.group(1), hash)
    val (dp, templates) = repo_files.partition { case (name, _) => name == "default.properties" }
    val defaults = dp.map { case (_, hash) => 
      http(show(repo, hash) >> { stm =>
        val p = new java.util.Properties
        p.load(stm)
        asMap(p: java.util.Map[AnyRef, AnyRef]): scala.collection.Map[AnyRef, AnyRef]
        (Map.empty[String, String] /: p.stringPropertyNames) { (m, k) =>
          m + (k -> p.getProperty(k))
        }
      } )
    }.headOption getOrElse Map.empty[String, String]
    val parameters = (defaults /: params) { 
      case (map, Param(key, value)) => map + (key -> value)
    }
    val base = new File(normalize(parameters.getOrElse("name", "My Project")))
    templates foreach { case (name, hash) =>
      import org.clapper.scalasti.StringTemplate
      val f = new File(base, name)
      f.getParentFile.mkdirs()
      http(show(repo, hash) >- { in =>
        val fw = new FileWriter(f)
        fw.write(new StringTemplate(in).setAttributes(parameters).toString)
        fw.close()
      })
    }
    new Exit(1)
  }
  class Exit(val code: Int) extends xsbti.Exit
  def http = new Http
  val gh = :/("github.com") / "api" / "v2" / "json"
  def show(repo: String, hash: String) = gh / "blob" / "show" / repo / hash
  def normalize(s: String) = s.toLowerCase.replaceAll("""\s+""", "-")
}
