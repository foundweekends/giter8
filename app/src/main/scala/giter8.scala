package giter8

class Giter8 extends xsbti.AppMain {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.collection.JavaConversions._
  import java.io.{File,FileWriter}

  val Root = "^src/main/g8/(.+)$".r
  val Param = """^--(\S+)=(.+)$""".r
  val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
  
  java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE)
  
  def run(config: xsbti.AppConfiguration) =
    (config.arguments.partition { s => Param.pattern.matcher(s).matches } match {
      case (params, Array(Repo(user, proj))) => inspect("%s/%s.g8".format(user, proj), params)
      case _ => Left("Usage: g8 <gituser/project.g8> [--param=value ...]")
    }) fold ({ error =>
      System.err.println("\n%s\n" format error)
      new Exit(0)
    }, { message =>
      println("\n%s\n" format message)
      new Exit(1)
    })

  def inspect(repo: String, params: Iterable[String]) =
    repo_files(repo).right.flatMap { repo_files =>
      val (default_props, templates) = repo_files.partition { 
        case (name, _) => name == "default.properties" 
      }
      val default_params = defaults(repo, default_props)
      val parameters = 
        if (params.isEmpty) interact(default_params)
        else (defaults(repo, default_props) /: params) { 
          case (map, Param(key, value)) if map.contains(key) => 
            map + (key -> value)
          case (map, Param(key, _)) =>
            println("Ignoring unregonized parameter: " + key)
          map
        }
      val base = new File(normalize(parameters.getOrElse("name", "My Project")))
      if (base.exists) 
        Left("This project directory already exists: " + base)
      else
        write(repo, templates, parameters, base)
    }

  def repo_files(repo: String) = try { Right(for {
    blobs <- http(gh / "blob" / "all" / repo / "master" ># ('blobs ? obj))
    JField(name, JString(hash)) <- blobs
    m <- Root.findFirstMatchIn(name)
  } yield (m.group(1), hash)) } catch {
    case StatusCode(404, _) => Left("Unable to find github repository: %s" format repo)
  }

  def defaults(repo: String, default_props: Iterable[(String, String)]) = 
    default_props.map { case (_, hash) => 
      http(show(repo, hash) >> { stm =>
        val p = new java.util.Properties
        p.load(stm)
        (Map.empty[String, String] /: p.stringPropertyNames) { (m, k) =>
          m + (k -> p.getProperty(k))
        }
      } )
    }.headOption getOrElse Map.empty[String, String]

  def interact(params: Map[String, String]) = {
    val (desc, others) = params partition { case (k,_) => k == "description" }
    desc.values.foreach { d => 
      @scala.annotation.tailrec 
      def liner(cursor: Int, rem: Iterable[String]) {
        if (!rem.isEmpty) {
          val next = cursor + 1 + rem.head.length
          if (next > 70) {
            println()
            liner(0, rem)
          } else {
            print(rem.head + " ")
            liner(next, rem.tail)
          }
        }
      }
      println()
      liner(0, d.split(" "))
      println("\n")
    }
    others map { case (k,v) =>
      val in = Console.readLine("%s [%s]: ", k,v).trim
      (k, if (in.isEmpty) v else in)
    }
  }

  def write(repo: String, templates: Iterable[(String, String)], parameters: Map[String,String], base: File) = {
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
    Right("Created project in %s" format base.toString)
  }

  class Exit(val code: Int) extends xsbti.Exit
  def http = new Http {
    override lazy val log = new dispatch.Logger {
      val jdklog = java.util.logging.Logger.getLogger("dispatch")
      def info(msg: String, items: Any*) { 
        jdklog.info(msg.format(items: _*)) 
      }
    }
  }
  val gh = :/("github.com") / "api" / "v2" / "json"
  def show(repo: String, hash: String) = gh / "blob" / "show" / repo / hash
  def normalize(s: String) = s.toLowerCase.replaceAll("""\s+""", "-")
}
