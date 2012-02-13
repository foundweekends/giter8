package giter8

import sbt._

object G8 {
  import scala.util.control.Exception.allCatch
  import org.clapper.scalasti.StringTemplate

  private val renderer = new StringRenderer

  def apply(fromMapping: Seq[(File,String)], toPath: File, parameters: Map[String,String], log: Logger): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap { case (in, relative) =>
      apply(in, expandPath(relative, toPath, parameters), parameters, log)
    }  
  def apply(in: File, out: File, parameters: Map[String,String], log: Logger): Seq[File] = {
    log.debug("Applying " + in)
    import java.nio.charset.{MalformedInputException, Charset}
    
    allCatch opt {
      if (verbatim(in, parameters)) IO.copyFile(in, out) 
      else {
        val template = IO.read(in, Charset forName "UTF-8")
        IO.write(out, new StringTemplate(template).setAttributes(parameters).registerRenderer(renderer).toString)
      }
    } getOrElse {
      log.info("Unable to parse template %s, copying unmodified" format in)
      IO.copyFile(in, out)      
    }
    allCatch opt {
      if (in.canExecute) out.setExecutable(true)
    }
    Seq(out)
  }
  
  private def verbatim(file: File, parameters: Map[String,String]): Boolean =
    parameters.get("verbatim") map { s => globMatch(file, s.split(' ').toSeq) } getOrElse {false}
  private def globMatch(file: File, patterns: Seq[String]): Boolean =
    patterns exists { globRegex(_).findFirstIn(file.getName).isDefined }
  private def globRegex(pattern: String) = "^%s$".format(pattern flatMap {
    case '*' => """.*"""
    case '?' => """."""
    case '.' => """\."""
    case x => x.toString
  }).r  
  private def expandPath(relative: String, toPath: File, parameters: Map[String,String]): File = {
    val fileParams = Map(parameters.toSeq map {
      case (k, v) if k == "package" => (k, v.replaceAll("""\.""", System.getProperty("file.separator") match {
          case """\"""  => """\\"""
          case sep => sep
        }))
      case x => x
    }: _*)

    new File(toPath, new StringTemplate(formatize(relative)).setAttributes(fileParams).registerRenderer(renderer).toString)
  }
  private def formatize(s: String) = s.replaceAll("""\$(\w+)__(\w+)\$""", """\$$1;format="$2"\$""")
}

class StringRenderer extends org.clapper.scalasti.AttributeRenderer[String] {
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
    case "packaged" | "package-dir"  => packageDir(value)
    case _                           => value
  }

  def decapitalize(s: String) = if (s.isEmpty) s else s(0).toLower + s.substring(1)
  def startCase(s: String) = s.toLowerCase.split(" ").map(_.capitalize).mkString(" ")
  def wordOnly(s: String) = s.replaceAll("""\W""", "")
  def upperCamel(s: String) = wordOnly(startCase(s))
  def lowerCamel(s: String) = decapitalize(upperCamel(s))
  def hyphenate(s: String) = s.replaceAll("""\s+""", "-")
  def normalize(s: String) = hyphenate(s.toLowerCase)
  def packageDir(s: String) = s.replace(".", System.getProperty("file.separator"))
}
