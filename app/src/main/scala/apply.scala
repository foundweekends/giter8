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

object GitRepo {

  import java.io.File
  import org.eclipse.jgit.api._
  import scala.io.Source
  import scala.util.control.Exception.{allCatch,catching}
  
  val TMP = new File(System.getProperty("java.io.tmpdir"), java.util.UUID.randomUUID().toString)
  val TEMPLATES_FOLDER = new File(TMP,"src/main/g8")
  
  def inspect(repo: String,
               branch: Option[String],
               arguments: Seq[String]) = {
    val tmpl = clone(repo, branch)

    val (ps, templates) = fetchInfo(tmpl)
    val parameters = interact(ps)
    val base = new File(
      parameters.get("name").map(G8.normalize).getOrElse(".")
    )

    write(repo, templates, parameters, base)
  }

  def fetchInfo(f: File) = {    
    def getFiles(filter: File => Boolean)(f: File): Stream[File] = 
      f #:: (if (f.isDirectory) f.listFiles().toStream.filter(filter).flatMap(getFiles(filter)) else Stream.empty)

    def getVisibleFiles = getFiles(!_.isHidden) _

    val fs = getVisibleFiles(f)
    val (propertiesFiles, templates) = fs.partition {
      _.getName == "default.properties"
    }

    val parameters = propertiesFiles.headOption.map{ f => 
      val s = Source.fromFile(f)
      // TODO: really read props
      Map("name" -> "biloute")
    }.getOrElse(Map.empty)
    
    val g8 = getVisibleFiles(TEMPLATES_FOLDER).filter(!_.isDirectory)
    (parameters, g8)

    // TODO: LS support
    // prepareDefaults(repo, propertiesFiles.headOption).right.map {
    //  defaults => (defaults, templates)
    // }
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

  def clone(repo: String, branch: Option[String]) = {
    val cmd = new CloneCommand()
    for(b <- branch)
      cmd.setBranch(b)
    cmd.setURI(repo)
    cmd.setDirectory(TMP)
    cmd.call()
    TMP
  }
  
  def write(repo: String,
            templates: Iterable[File],
            parameters: Map[String,String],
            base: File) = {
    import java.nio.charset.MalformedInputException
    val renderer = new StringRenderer
    
    templates.map{ in =>
      val relative =  TEMPLATES_FOLDER.toURI().relativize(in.toURI).getPath
      println(relative)
      val out = new File(base, relative)
      (in, out)
    }.foreach { case (in, out) =>
      if (out.exists) {
        println("Skipping existing file: %s" format out.toString)
      }
      else {
        out.getParentFile.mkdirs()
        if (G8.verbatim(out, parameters))
          None
        else {
          catching(classOf[MalformedInputException]).opt {
            Some(G8.write(out, Source.fromFile(in).mkString, parameters))
          }
        }
      }
    }

    Right("Applied %s in %s" format (repo, base.toString))
  }
}

