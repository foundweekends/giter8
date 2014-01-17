package giter8

import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.Charsets.UTF_8

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
      if (verbatim(in, parameters)) FileUtils.copyFile(in, out)
      else {
        write(out, FileUtils.readFileToString(in, "UTF-8"), parameters)
      }
    }
    catch {
      case e: Exception =>
        println("Falling back to file copy for %s: %s" format(in.toString, e.getMessage))
        FileUtils.copyFile(in, out)
    }
    allCatch opt {
      if (in.canExecute) out.setExecutable(true)
    }
    Seq(out)
  }

  def write(out: File, template: String, parameters: Map[String, String], append: Boolean = false) {
    val applied = new StringTemplate(template)
      .setAttributes(parameters)
      .registerRenderer(renderer)
      .toString
    FileUtils.writeStringToFile(out, applied, UTF_8, append)
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
  def snakeCase(s: String) = s.replaceAll("""[\s\.]+""", "_")
  def packageDir(s: String) = s.replace(".", System.getProperty("file.separator"))
  def addRandomId(s: String) = s + "-" + new java.math.BigInteger(256, new java.security.SecureRandom).toString(32)

}

case class Config(
  repo: String = "",
  branch: Option[String] = None,
  forceOverwrite: Boolean = false
)
object G8Helpers {
  import scala.util.control.Exception.catching

  val Param = """^--(\S+)=(.+)$""".r

  private def applyT(fetch: File => (Map[String, String], Stream[File], File, Option[File]), isScaffolding: Boolean = false)(tmpl: File, outputFolder: File, arguments: Seq[String] = Nil, forceOverwrite: Boolean = false) = {
    val (defaults, templates, templatesRoot, scaffoldsRoot) = fetch(tmpl)

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

    val r = write(templatesRoot, templates, parameters, base, isScaffolding, forceOverwrite)
    for(
      _ <- r.right;
      root <- scaffoldsRoot
    ) copyScaffolds(root, base)
    r
  }

  private def fetchProjectTemplateinfo = fetchInfo(_: File, Some("src/main/g8"), Some("src/main/scaffolds"))
  private def fetchRawTemplateinfo = fetchInfo(_: File, None, None)

  def applyTemplate = applyT(fetchProjectTemplateinfo) _
  def applyRaw = applyT(fetchRawTemplateinfo, isScaffolding = true) _

  private def getFiles(filter: File => Boolean)(f: File): Stream[File] =
    f #:: (if (f.isDirectory) f.listFiles().toStream.filter(filter).flatMap(getFiles(filter)) else Stream.empty)

  private def getVisibleFiles = getFiles(!_.isHidden) _

  /**
  * Extract params, template files, and scaffolding folder based on the conventionnal project structure
  */
  def fetchInfo(f: File, tmplFolder: Option[String], scaffoldFolder: Option[String]) = {
    import java.io.FileInputStream

    val templatesRoot = tmplFolder.map(new File(f, _)).getOrElse(f)
    val fs = getFiles(_ => true)(templatesRoot)
    val propertiesLoc = new File(templatesRoot, "default.properties")
    val scaffoldsRoot = scaffoldFolder.map(new File(f, _))

    val (propertiesFiles, tmpls) = fs.partition {
      _ == propertiesLoc
    }

    val parameters = propertiesFiles.headOption.map{ f =>
      val props = readProps(new FileInputStream(f))
      Ls.lookup(props).right.toOption.getOrElse(props)
    }.getOrElse(Map.empty)

    val g8templates = tmpls.filter(!_.isDirectory)

    (parameters, g8templates, templatesRoot, scaffoldsRoot)
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

    val fixed = Set("verbatim")
    others map { case (k,v) =>
      if (fixed.contains(k))
        (k, v)
      else {
        val in = Console.readLine("%s [%s]: ", k,v).trim
        (k, if (in.isEmpty) v else in)
      }
    }
  }

  private def relativize(in: File, from: File) = from.toURI().relativize(in.toURI).getPath

  def write(tmpl: File,
            templates: Iterable[File],
            parameters: Map[String,String],
            base: File,
            isScaffolding: Boolean,
            forceOverwrite: Boolean) = {

    import java.nio.charset.MalformedInputException
    val renderer = new StringRenderer

    templates.map{ in =>
      val name =  relativize(in, tmpl)
      val out = G8.expandPath(name, base, parameters)
      (in, out)
    }.foreach { case (in, out) =>
      val existingScaffoldingAction = if (out.exists && isScaffolding) {
          println(out.getCanonicalPath+" already exists") 
          print("do you want to append, override or skip existing file? [O/a/s] ")
          Console.readLine match {
            case a if a == "a"  => Some(true)
            case a if a == "o" || a == ""  => Some(false)
            case _ => None
          }
        } else None

      if (out.exists && 
          existingScaffoldingAction.isDefined == false &&
          forceOverwrite == false) {
        println("Skipping existing file: %s" format out.toString)
      }
      else  {
        out.getParentFile.mkdirs()
        if (G8.verbatim(out, parameters))
          FileUtils.copyFile(in, out)
        else {
          catching(classOf[MalformedInputException]).opt {
            Some(G8.write(out, FileUtils.readFileToString(in, UTF_8), parameters, append = existingScaffoldingAction.getOrElse(false)))
          }.getOrElse {
            if (existingScaffoldingAction.getOrElse(false)) {
              val existing = FileUtils.readFileToString(in, UTF_8)
              FileUtils.write(out, existing, UTF_8, true)
            } else {
              FileUtils.copyFile(in, out)
            }
          }
        }
        if (in.canExecute) {
          out.setExecutable(true)
        }
      }
    }

    Right("Template applied in %s" format (base.toString))
  }

  def copyScaffolds(sf: File, output: File) {

    val scaffolds = if(sf.exists) Some(getFiles(_ => true)(sf)) else None

    for(
      fs <- scaffolds;
      f <- fs if !f.isDirectory
    ) {
      // Copy scaffolding recipes
      val realProjectRoot = getVisibleFiles(output)
        .filter(_.isDirectory)
        .filter(_.getName == "project")
        .map(_.getParentFile)
        .headOption
        .getOrElse(output)

      val hidden = new File(realProjectRoot, ".g8")
      val name = relativize(f, sf)
      val out = new File(hidden, name)
      FileUtils.copyFile(f, out)
    }
  }


  def readProps(stm: java.io.InputStream) = {
    import scala.collection.JavaConversions._
    val p = new java.util.Properties
    p.load(stm)
    stm.close()
    (Map.empty[String, String] /: p.propertyNames) { (m, k) =>
      m + (k.toString -> p.getProperty(k.toString))
    }
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
