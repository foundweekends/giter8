package giter8

case class FileInfo(name: String, hash: String, mode: String)

trait Apply extends Defaults with GitRepo { self: Giter8 =>
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

trait GitRepo extends Defaults { self: Giter8 =>

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

    val (defaults, templates) = fetchInfo(tmpl)
    
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

  def fetchInfo(f: File) = {    
    import java.io.FileInputStream

    def getFiles(filter: File => Boolean)(f: File): Stream[File] = 
      f #:: (if (f.isDirectory) f.listFiles().toStream.filter(filter).flatMap(getFiles(filter)) else Stream.empty)

    def getVisibleFiles = getFiles(!_.isHidden) _

    val fs = getVisibleFiles(f)
    val (propertiesFiles, _) = fs.partition {
      _.getName == "default.properties"
    }

    val parameters = propertiesFiles.headOption.map{ f => 
      val props = GIO.readProps(new FileInputStream(f))
      Ls.lookup(props).right.toOption.getOrElse(props)
    }.getOrElse(Map.empty)
    
    val g8templates = getVisibleFiles(TEMPLATES_FOLDER).filter(!_.isDirectory)
    (parameters, g8templates)
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

  // TODO: expections handling
  def clone(repo: String, branch: Option[String]) = {
    val cmd = new CloneCommand()
    for(b <- branch)
      cmd.setBranch(b)
    cmd.setURI(repo)
    cmd.setDirectory(TMP)
    cmd.call()
    TMP.deleteOnExit()
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
          }.getOrElse {
            GIO.copyFile(in, out)
          }
        }
      }
    }

    Right("Applied %s in %s" format (repo, base.toString))
  }
}

