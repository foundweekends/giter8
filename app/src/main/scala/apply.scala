package giter8

case class FileInfo(name: String, hash: String, mode: String)

trait Apply extends Defaults { self: Giter8 =>
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.collection.JavaConversions._
  import java.io.{File,FileWriter,FileOutputStream}
  import scala.util.control.Exception.{allCatch,catching}

  val Root = "^src/main/g8/(.+)$".r
  val Param = """^--(\S+)=(.+)$""".r
  val Text = """^(text|application)/.+""".r
  val DefaultBranch = "master"

  def inspect(repo: String,
              branch: Option[String],
              arguments: Seq[String]) =
    fetchInfo(repo, branch).right.flatMap {
      case (defaults, templates) =>
        val parameters = arguments.headOption.map { _ =>
          (defaults /: arguments) {
            case (map, Param(key, value)) if map.contains(key) =>
              map + (key -> value)
            case (map, Param(key, _)) =>
              println("Ignoring unrecognized parameter: " + key)
              map
          }
        }.getOrElse { interact(defaults) }
        val base = new File(
          parameters.get("name").map(G8.normalize).getOrElse(".")
        )
        write(repo, templates, parameters, base)
    }

  def fetchInfo(repo: String, branch: Option[String]) = {
    repoFiles(repo, branch.getOrElse(DefaultBranch)).right.flatMap { files =>
      val (propertiesFiles, templates) = files.partition {
        _.name == "default.properties"
      }
      prepareDefaults(repo, propertiesFiles.headOption).right.map {
        defaults => (defaults, templates)
      }
    }
  }

  def repoFiles(repo: String, branch: String): Either[String,Seq[FileInfo]] =
    allCatch.either {
      val shas = 
        http(gh / repo / "git" / "refs" / "heads" / branch ># { js =>
          for {
            JField("object", JObject(obj)) <- js
            JField("sha", JString(sha)) <- obj
          } yield sha
        })
      shas.flatMap { sha =>
        http(gh / repo / "git" / "trees" / sha <<? Map(
          "recursive" -> "1"
        ) ># { js =>
          for {
            JField("tree", JArray(tree)) <- js
            JObject(blob) <- tree
            JField("type", JString("blob")) <- blob
            JField("path", JString(name)) <- blob
            JField("sha", JString(hash)) <- blob
            JField("mode", JString(mode)) <- blob
            m <- Root.findFirstMatchIn(name)
          } yield FileInfo(m.group(1), hash, mode)
        })
      }
    }.left.flatMap {
      case StatusCode(404, _) => Right(Seq.empty)
      case e => Left("Exception fetching from github " + e.getMessage)
    }.right.flatMap { seq =>
      if (seq.isEmpty)
        Left("Unable to find github repository: %s (%s)".format(repo, branch))
      else
        Right(seq)
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

  def write(repo: String,
            templates: Iterable[FileInfo],
            parameters: Map[String,String],
            base: File) = {
    import java.nio.charset.MalformedInputException
    val renderer = new StringRenderer
    templates.foreach { case FileInfo(name, hash, mode) =>
      val f = G8.expandPath(name, base, parameters)
      if (f.exists)
        println("Skipping existing file: %s" format f.toString)
      else {
        f.getParentFile.mkdirs()
        (if (G8.verbatim(f, parameters)) None
        else catching(classOf[MalformedInputException]).opt {
          http(show(repo, hash) >- { in =>
            Some(G8.write(f, in, parameters))
          })
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
  
  def setFileMode(f: File, mode: String) = allCatch opt {
    if ((mode(3).toString.toInt & 0x1) > 0) {
      f.setExecutable(true)
    }
  }
  def show(repo: String, hash: String) =
    gh / repo / "git" / "blobs" / hash <:< Map(
      "Accept" -> "application/vnd.github.raw"
    )
  private def use[C <: { def close(): Unit }, T](c: C)(f: C => T): T =
    try { f(c) } finally { c.close() }
}

