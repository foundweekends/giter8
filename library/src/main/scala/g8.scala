package giter8

import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.Charsets.UTF_8
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributes
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils


object G8 {
  import scala.util.control.Exception.allCatch
  import org.clapper.scalasti.StringTemplate

  /** Properties in the order they were created/defined */
  type OrderedProperties  = List[(String, String)]
  object OrderedProperties {
    val empty = List.empty[(String, String)]
  }

  /** G8 template properties which have been fully resolved, i.e. defaults replaced by user input, ready for insertion into template */
  type ResolvedProperties = Map[String, String]
  object ResolvedProperties {
    val empty = Map.empty[String, String]
  }

  /** dispatch http client used by the `ls` integration library, for
    * general reuse within giter8. */
  lazy val http = ls.DefaultClient.http

  /**
    * A function which will return the resolved value of a property given the properties resolved thus far.
    * This is a bit more general than was needed for resolving "dynamic defaults". I did it this way so it's
    * possible to have other ValueF definitions which perform arbitrary logic given previously defined properties.
    */
  type ValueF = ResolvedProperties => String

  /** The ValueF implementation for handling default properties.  It performs formatted substitution on any properties found. */
  case class DefaultValueF(default:String) extends ValueF {
    override def apply(resolved:ResolvedProperties):String = new StringTemplate(default)
      .setAttributes(resolved)
      .registerRenderer(renderer)
      .toString
  }

  /** Properties which have not been resolved. I.e., ValueF() has not been evaluated */
  type UnresolvedProperties = List[(String, ValueF)]
  object UnresolvedProperties {
    val empty = List.empty[(String, ValueF)]
  }

  private val renderer = new StringRenderer

  def apply(fromMapping: Seq[(File,String)], toPath: File, parameters: Map[String,String]): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap { case (in, relative) =>
      apply(in, expandPath(relative, toPath, parameters), parameters)
    }

  def apply(in: File, out: File, parameters: Map[String,String]) = {
    try {
      if (verbatim(in, parameters)) FileUtils.copyFile(in, out)
      else {
        write(in, out, parameters, false)
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

  def write(in: File, out: File, parameters: Map[String, String], append: Boolean) {
    Option(PlexusIoResourceAttributeUtils.getFileAttributes(in)) match {
      case Some(attr) =>
        val mode = attr.getOctalMode
        write(out, FileUtils.readFileToString(in, "UTF-8"), parameters, append)
        util.Try(ArchiveEntryUtils.chmod(out, mode, new ConsoleLogger(Logger.LEVEL_ERROR, "")))
      case None =>
        // PlexusIoResourceAttributes is not available for some OS'es such as windows
        write(out, FileUtils.readFileToString(in, "UTF-8"), parameters, append)
    }
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
  tag: Option[String] = None,
  forceOverwrite: Boolean = false,
  search: Boolean = false
)
object G8Helpers {
  import scala.util.control.Exception.catching
  import G8._

  val Param = """^--(\S+)=(.+)$""".r

  private def applyT(
    fetch: File => Either[String, (UnresolvedProperties, Stream[File], File, Option[File])],
    isScaffolding: Boolean = false
  )(
    tmpl: File,
    outputFolder: File,
    arguments: Seq[String] = Nil,
    forceOverwrite: Boolean = false
  ) = {
    fetch(tmpl).right.flatMap {
      case (defaults, templates, templatesRoot, scaffoldsRoot) =>
        val parameters = consoleParams(defaults, arguments).getOrElse {
          interact(defaults)
        }

        val base = new File(
          outputFolder,
          parameters.get("name").map(G8.normalize).getOrElse(".")
        )

        val r = write(templatesRoot, templates, parameters, base, isScaffolding,
          forceOverwrite)
        for {
          _ <- r.right
          root <- scaffoldsRoot
        } copyScaffolds(root, base)
        r
      }
  }

  private def fetchProjectTemplateinfo(file: File) =
    fetchInfo(file: File, Some("src/main/g8"), Some("src/main/scaffolds"))

  private def fetchRawTemplateinfo(file: File) =
    fetchInfo(file, None, None)

  def applyTemplate = applyT(fetchProjectTemplateinfo) _
  def applyRaw = applyT(fetchRawTemplateinfo, isScaffolding = true) _

  private def getFiles(filter: File => Boolean)(f: File): Stream[File] =
    f #:: (if (f.isDirectory) f.listFiles().toStream.filter(filter).flatMap(getFiles(filter)) else Stream.empty)

  private def getVisibleFiles = getFiles(!_.isHidden) _

  /** transforms any ls() and maven() property operations to the latest
    * version number reported by that service. */
  def transformProps(props: G8.OrderedProperties): Either[String, G8.OrderedProperties] =
    Ls.lookup(props).right.flatMap(Maven.lookup)

  /**
  * Extract params, template files, and scaffolding folder based on the conventionnal project structure
  */
  private def fetchInfo(
    f: File,
    tmplFolder: Option[String],
    scaffoldFolder: Option[String]
  ) = {
    import java.io.FileInputStream

    val templatesRoot = tmplFolder.map(new File(f, _)).getOrElse(f)
    val fs = getFiles(_ => true)(templatesRoot)
    val propertiesLoc = new File(templatesRoot, "default.properties")
    val scaffoldsRoot = scaffoldFolder.map(new File(f, _))

    val (propertiesFiles, tmpls) = fs.partition {
      _ == propertiesLoc
    }

    val parametersEither = propertiesFiles.headOption.map{ f =>
      val props = readProps(new FileInputStream(f))
      val transformed = transformProps(props)
      transformed.right.map { _.map { case (k, v) => (k, DefaultValueF(v)) } }
    }.getOrElse(Right(UnresolvedProperties.empty))

    val g8templates = tmpls.filter(!_.isDirectory)

    for (parameters <- parametersEither.right) yield
      (parameters, g8templates, templatesRoot, scaffoldsRoot)
  }

  def consoleParams(defaults: UnresolvedProperties, arguments: Seq[String]) = {
    arguments.headOption.map { _ =>
      val specified = (ResolvedProperties.empty /: arguments) {
        case (map, Param(key, value)) if defaults.map(_._1).contains(key) =>
          map + (key -> value)
        case (map, Param(key, _)) =>
          println("Ignoring unrecognized parameter: " + key)
          map
      }

      // Add anything from defaults that wasn't picked up as an argument from the console.
      defaults.foldLeft(specified) { case (resolved, (k, f)) =>
        if(!resolved.contains(k)) resolved + (k -> f(resolved))
        else resolved
      }
    }
  }

  def interact(params: UnresolvedProperties):ResolvedProperties = {
    val (desc, others) = params partition { case (k,_) => k == "description" }

    desc.foreach { d =>
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
      liner(0, d._2(ResolvedProperties.empty).split(" "))
      println("\n")
    }

    val fixed = Set("verbatim")
    val renderer = new StringRenderer

    others.foldLeft(ResolvedProperties.empty) { case (resolved, (k,f)) =>
      resolved + (
        if (fixed.contains(k))
          k -> f(resolved)
        else {
          val default = f(resolved)
          printf("%s [%s]: ", k, default)
          Console.flush() // Gotta flush for Windows console!
          val in = Console.readLine().trim
          (k, if (in.isEmpty) default else in)
        }
      )
    }.toMap
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
            Some(G8.write(in, out, parameters, append = existingScaffoldingAction.getOrElse(false)))
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

  def readProps(stm: java.io.InputStream):G8.OrderedProperties = {
    val p = new LinkedListProperties
    p.load(stm)
    stm.close()
    (OrderedProperties.empty /: p.keyList) { (l, k) =>
      l :+ (k -> p.getProperty(k))
    }
  }
}

/** Hacked override of java.util.Properties for the sake of getting the properties in the order they are specified in the file */
private [giter8] class LinkedListProperties extends java.util.Properties {
  var keyList = List.empty[String]

  override def put(k:Object, v:Object) = {
    keyList = keyList :+ k.toString
    super.put(k, v)
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
