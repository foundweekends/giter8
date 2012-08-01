package giter8

import java.io.File

object G8 {
  import scala.util.control.Exception.allCatch
  import org.clapper.scalasti.StringTemplate

  private val renderer = new StringRenderer

  def apply(fromMapping: Seq[(File,String)], toPath: File, parameters: Map[String,String]): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap { case (in, relative) =>
      apply(in, expandPath(relative, toPath, parameters), parameters)
    }  

  def apply(in: File, out: File, parameters: Map[String,String]) = {

    try {
      if (verbatim(in, parameters)) GIO.copyFile(in, out) 
      else {
        write(out, GIO.read(in, "UTF-8"), parameters)
      }
    }
    catch {
      case e: Exception =>
        println("Falling back to file copy for %s: %s" format(in.toString, e.getMessage))
        GIO.copyFile(in, out)
    }
    allCatch opt {
      if (in.canExecute) out.setExecutable(true)
    }
    Seq(out)
  }

  def write(out: File, template: String, parameters: Map[String, String]) {
    val applied = new StringTemplate(template)
      .setAttributes(parameters)
      .registerRenderer(renderer)
      .toString
    GIO.write(out, applied, "UTF-8")
  }
  
  def verbatim(file: File, parameters: Map[String,String]): Boolean =
    parameters.get("verbatim") map { s => globMatch(file, s.split(' ').toSeq) } getOrElse {false}
  private def globMatch(file: File, patterns: Seq[String]): Boolean =
    patterns exists { globRegex(_).findFirstIn(file.getName).isDefined }
  private def globRegex(pattern: String) = "^%s$".format(pattern flatMap {
    case '*' => """.*"""
    case '?' => """."""
    case '.' => """\."""
    case x => x.toString
  }).r  
  def expandPath(relative: String, toPath: File, parameters: Map[String,String]): File = {
    val fileParams = Map(parameters.toSeq map {
      case (k, v) if k == "package" => (k, v.replaceAll("""\.""", System.getProperty("file.separator") match {
          case "\\"  => "\\\\"
          case sep => sep
        }))
      case x => x
    }: _*)

    new File(toPath, new StringTemplate(formatize(relative)).setAttributes(fileParams).registerRenderer(renderer).toString)
  }
  private def formatize(s: String) = s.replaceAll("""\$(\w+)__(\w+)\$""", """\$$1;format="$2"\$""")

  def decapitalize(s: String) = if (s.isEmpty) s else s(0).toLower + s.substring(1)
  def startCase(s: String) = s.toLowerCase.split(" ").map(_.capitalize).mkString(" ")
  def wordOnly(s: String) = s.replaceAll("""\W""", "")
  def upperCamel(s: String) = wordOnly(startCase(s))
  def lowerCamel(s: String) = decapitalize(upperCamel(s))
  def hyphenate(s: String) = s.replaceAll("""\s+""", "-")
  def normalize(s: String) = hyphenate(s.toLowerCase)
  def snakeCase(s: String) = s.replaceAll("""\s+""", "_")
  def packageDir(s: String) = s.replace(".", System.getProperty("file.separator"))
  def addRandomId(s: String) = s + "-" + new java.math.BigInteger(256, new java.security.SecureRandom).toString(32)

}

object G8Helpers {
  import scala.util.control.Exception.catching
  import scala.io.Source

  object Regs {
    val Param = """^--(\S+)=(.+)$""".r
    val Repo = """^(\S+)/(\S+?)(?:\.g8)?$""".r
    val Branch = """^-(b|-branch)$""".r
    val RemoteTemplates = """^-(l|-list)$""".r
    val Git = """^(.*\.g8\.git)$""".r
    val Local = """^file://(\S+)$""".r
  }
  
  import Regs._

  private def applyT(fetch: File => (Map[String, String], Stream[File], File))(tmpl: File, outputFolder: File, arguments: Seq[String] = Nil) = {
    val (defaults, templates, templatesRoot) = fetch(tmpl)
    
    val parameters = arguments.headOption.map { _ =>
      (defaults /: arguments) {
        case (map, Param(key, value)) if map.contains(key) =>
          map + (key -> value)
        case (map, Param(key, _)) =>
          println("Ignoring unrecognized parameter: " + key)
          map
      }
    }.getOrElse { interact(defaults) }
    
    val base = new File(outputFolder, parameters.get("name").map(G8.normalize).getOrElse("."))

    write(templatesRoot, templates, parameters, base)
  }

  private def fetchProjectTemplateinfo = fetchInfo(_: File, Some("src/main/g8"))
  private def fetchRawTemplateinfo = fetchInfo(_: File, None)
  def applyTemplate = applyT(fetchProjectTemplateinfo) _
  def applyRaw = applyT(fetchRawTemplateinfo) _

  private def getFiles(filter: File => Boolean)(f: File): Stream[File] = 
    f #:: (if (f.isDirectory) f.listFiles().toStream.filter(filter).flatMap(getFiles(filter)) else Stream.empty)

  private def getVisibleFiles = getFiles(!_.isHidden) _

  def fetchInfo(f: File, tmplFolder: Option[String]) = {    
    import java.io.FileInputStream

    val templatesRoot = tmplFolder.map(new File(f, _)).getOrElse(f)
    val propertiesLoc = new File(templatesRoot, "default.properties")
    val fs = getVisibleFiles(templatesRoot)

    val (propertiesFiles, tmpls) = fs.partition {
      _ == propertiesLoc
    }

    val parameters = propertiesFiles.headOption.map{ f => 
      val props = GIO.readProps(new FileInputStream(f))
      Ls.lookup(props).right.toOption.getOrElse(props)
    }.getOrElse(Map.empty)
    
    val g8templates = tmpls.filter(!_.isDirectory)
    (parameters, g8templates, templatesRoot)
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
  
  private def relativize(in: File, from: File) = from.toURI().relativize(in.toURI).getPath
  
  def write(tmpl: File,
            templates: Iterable[File],
            parameters: Map[String,String],
            base: File) = {

    import java.nio.charset.MalformedInputException
    val renderer = new StringRenderer

    templates.map{ in =>
      val name =  relativize(in, tmpl)
      val out = G8.expandPath(name, base, parameters)
      (in, out)
    }.foreach { case (in, out) =>
      if (out.exists) {
        println("Skipping existing file: %s" format out.toString)
      }
      else {
        out.getParentFile.mkdirs()
        if (G8.verbatim(out, parameters))
          GIO.copyFile(in, out)
        else {
          catching(classOf[MalformedInputException]).opt {
            Some(G8.write(out, Source.fromFile(in).mkString, parameters))
          }.getOrElse {
            GIO.copyFile(in, out)
          }
        }
      }
    }

    Right("Template applied in %s" format (base.toString))
  }

}

class StringRenderer extends org.clapper.scalasti.AttributeRenderer[String] {
  import G8._
  def toString(value: String): String = value

  override def toString(value: String, formatName: String): String = {
    val formats = formatName.split(",").map(_.trim)
    formats.foldLeft(value)(format)
  }

  def format(value: String, formatName: String): String = formatName match {
    case "upper"    | "uppercase"    => value.toUpperCase
    case "lower"    | "lowercase"    => value.toLowerCase
    case "cap"      | "capitalize"   => value.capitalize
    case "decap"    | "decapitalize" => decapitalize(value)
    case "start"    | "start-case"   => startCase(value)
    case "word"     | "word-only"    => wordOnly(value)
    case "Camel"    | "upper-camel"  => upperCamel(value)
    case "camel"    | "lower-camel"  => lowerCamel(value)
    case "hyphen"   | "hyphenate"    => hyphenate(value)
    case "norm"     | "normalize"    => normalize(value)
    case "snake"    | "snake-case"   => snakeCase(value)
    case "packaged" | "package-dir"  => packageDir(value)
    case "random"   | "generate-random" => addRandomId(value)
    case _                           => value
  }
}
