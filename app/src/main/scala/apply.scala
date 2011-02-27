package giter8

trait Apply { self: Giter8 =>
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.collection.JavaConversions._
  import java.io.{File,FileWriter}

  val Root = "^src/main/g8/(.+)$".r
  val Param = """^--(\S+)=(.+)$""".r

  def inspect(repo: String, params: Iterable[String]) =
    repoFiles(repo).right.flatMap { repo_files =>
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
      val base = new File(parameters.get("name").map(normalize).getOrElse("."))
      write(repo, templates, parameters, base)
    }

  def repoFiles(repo: String) = try { Right(for {
    blobs <- http(gh / "blob" / "all" / repo / "master" ># ('blobs ? obj))
    JField(name, JString(hash)) <- blobs
    m <- Root.findFirstMatchIn(name)
  } yield (m.group(1), hash)) } catch {
    case StatusCode(404, _) => Left("Unable to find github repository: %s" format repo)
  }

  def defaults(repo: String, default_props: Iterable[(String, String)]) = 
    default_props.map { case (_, hash) => 
      http(show(repo, hash) >> readProps _ )
    }.headOption getOrElse Map.empty[String, String]

  def readProps(stm: java.io.InputStream) = {
    import scala.collection.JavaConversions._
    val p = new java.util.Properties
    p.load(stm)
    stm.close()
    (Map.empty[String, String] /: p.propertyNames) { (m, k) =>
      m + (k.toString -> p.getProperty(k.toString))
    }
  }

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
      val f = new File(base, new StringTemplate(name).setAttributes(parameters).toString)
      if (f.exists)
        println("Skipping existing file: " + f.toString)
      else {
        f.getParentFile.mkdirs()
        http(show(repo, hash) >- { in =>
          val fw = new FileWriter(f)
          fw.write(new StringTemplate(in).setAttributes(parameters).toString)
          fw.close()
        })
      }
    }
    Right("Applied %s in %s" format (repo, base.toString))
  }
  def normalize(s: String) = s.toLowerCase.replaceAll("""\s+""", "-")
  def show(repo: String, hash: String) = gh / "blob" / "show" / repo / hash
}
