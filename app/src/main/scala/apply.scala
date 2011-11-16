package giter8

trait Apply { self: Giter8 =>
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.collection.JavaConversions._
  import java.io.{File,FileWriter,FileOutputStream}
  import scala.util.control.Exception.allCatch

  val Root = "^src/main/g8/(.+)$".r
  val Param = """^--(\S+)=(.+)$""".r
  val Text = """^(text|application)/.+""".r
  val DefaultBranch = "master"

  def inspect(repo: String, branch: Option[String], params: Iterable[String]) =
    repoFiles(repo, branch.getOrElse(DefaultBranch)).right.flatMap { repo_files =>
      repo_files match {
         case Nil => Left("Unable to find github repository: %s (%s)" format(repo, branch.getOrElse(DefaultBranch)))
         case xs =>
           val (default_props, templates) = xs.partition {
             case (name, _, _, _) => name == "default.properties"
           }
           val default_params = defaults(repo, default_props)
           val parameters =
             if (params.isEmpty) interact(default_params)
             else (defaults(repo, default_props) /: params) {
               case (map, Param(key, value)) if map.contains(key) =>
                 map + (key -> value)
               case (map, Param(key, _)) =>
                 println("Ignoring unrecognized parameter: " + key)
                 map
             }
           val base = new File(parameters.get("name").map(normalize).getOrElse("."))
           write(repo, templates, parameters, base)
      }
    }

  def repoFiles(repo: String, branch: String) = try { Right(for {
    blobs <- http(gh / "blob" / "full" / repo / branch ># ('blobs ? ary))
    JObject(blob) <- blobs
    JField("name", JString(name)) <- blob
    JField("sha", JString(hash)) <- blob
    JField("mime_type", JString(mime)) <- blob
    JField("mode", JString(mode)) <- blob
    m <- Root.findFirstMatchIn(name)
  } yield (m.group(1), hash, mime, mode)) } catch {
    case StatusCode(404, _) => Left("Unable to find github repository: %s (%s)" format(repo, branch))
  }

  def defaults(repo: String, default_props: Iterable[(String, String, String, String)]) =
    default_props.map { case (_, hash, _, _) =>
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

  def write(repo: String, templates: Iterable[(String, String, String, String)], parameters: Map[String,String], base: File) = {
    templates foreach { case (name, hash, mime, mode) =>
      import org.clapper.scalasti.StringTemplate
      import java.nio.charset.MalformedInputException
      val fileParams = Map(parameters.toSeq map {
        case (k, v) if k == "package" => (k, v.replaceAll("""\.""", System.getProperty("file.separator") match {
            case """\"""  => """\\"""
            case sep => sep
          }))
        case x => x
      }: _*)
      val f = new File(base, new StringTemplate(name).setAttributes(fileParams).toString)
      if (f.exists)
        println("Skipping existing file: %s" format f.toString)
      else {
        f.getParentFile.mkdirs()
        (mime match {
          case x if verbatim(f, parameters) => None
          case Text(_) =>
            try {
              http(show(repo, hash) >- { in =>
                use (new FileWriter(f)) { fw =>
                  fw.write(new StringTemplate(in).setAttributes(parameters).toString)
                  Some(())
                }
              })
            }
            catch {
              case e: MalformedInputException => None
            }
          case binary => None
        }) getOrElse {
          http(show(repo, hash) >> { is =>
            use(is) { in =>
              use(new FileOutputStream(f)) { out =>
                def consume(buf: Array[Byte]): Unit =
                  in.read(buf) match {
                    case -1 => ()
                    case n =>
                       out.write(buf, 0, n)
                       consume(buf)
                  }
                consume(new Array[Byte](1024))
              }
            }
          })
        }
        setFileMode(f, mode)
      }
    }
    Right("Applied %s in %s" format (repo, base.toString))
  }
  
  def verbatim(file: File, parameters: Map[String,String]): Boolean =
    parameters.get("verbatim") map { s => globMatch(file, s.split(' ').toSeq) } getOrElse {false}
  def globMatch(file: File, patterns: Seq[String]): Boolean =
    patterns exists { globRegex(_).findFirstIn(file.getName).isDefined }
  def globRegex(pattern: String) = "^%s$".format(pattern flatMap {
    case '*' => """.*"""
    case '?' => """."""
    case '.' => """\."""
    case x => x.toString
  }).r  
  def setFileMode(f: File, mode: String) = allCatch opt {
    if ((mode(3).toString.toInt & 0x4) > 0) {
      f.setExecutable(true)
    }
  }
  def normalize(s: String) = s.toLowerCase.replaceAll("""\s+""", "-")
  def show(repo: String, hash: String) = gh / "blob" / "show" / repo / hash
  private def use[C <: { def close(): Unit }, T](c: C)(f: C => T): T =
    try { f(c) } finally { c.close() }
}
