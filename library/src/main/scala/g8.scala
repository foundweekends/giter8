/*
 * Original implementation (C) 2010-2015 Nathan Hamblen and contributors
 * Adapted and extended in 2016 by foundweekends project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package giter8

import java.io.{File, InputStream}
import java.util.regex.Matcher
import java.util.{Locale, Properties}

import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils
import org.stringtemplate.v4.compiler.STException
import org.stringtemplate.v4.misc.STMessage

import scala.collection.mutable
import scala.util.Try
import scala.util.control.Exception.{allCatch, catching}
import scala.util.control.NonFatal
import java.nio.charset.StandardCharsets

object G8 {
  import org.stringtemplate.v4.{AttributeRenderer, ST, STGroup, STErrorListener}

  /** Properties in the order they were created/defined */
  type OrderedProperties = List[(String, String)]

  /** Properties which have not been resolved. I.e., ValueF() has not been evaluated */
  type UnresolvedProperties = List[(String, ValueF)]
  object UnresolvedProperties {
    val empty = List.empty[(String, ValueF)]
  }

  /** G8 template properties which have been fully resolved, i.e. defaults replaced by user input, ready for insertion
    * into template
    */
  type ResolvedProperties = Map[String, String]
  object ResolvedProperties {
    val empty = Map.empty[String, String]
  }

  /** A function which will return the resolved value of a property given the properties resolved thus far. This is a
    * bit more general than was needed for resolving "dynamic defaults". I did it this way so it's possible to have
    * other ValueF definitions which perform arbitrary logic given previously defined properties.
    */
  type ValueF = ResolvedProperties => String

  // Called from JgitHelper
  def fromDirectory(
      templateDirectory: File,
      workingDirectory: File,
      arguments: Seq[String],
      forceOverwrite: Boolean,
      outputDirectory: Option[File]
  ): Either[String, String] =
    applyT((file: File) =>
      fetchInfo(
        file: File,
        defaultTemplatePaths,
        List(path("src") / "main" / "scaffolds", path("project") / "src" / "main" / "scaffolds")
      )
    )(templateDirectory, workingDirectory, arguments, forceOverwrite, outputDirectory)

  // Called from ScaffoldPlugin
  def fromDirectoryRaw(
      templateDirectory: File,
      outputDirectory: File,
      arguments: Seq[String],
      forceOverwrite: Boolean
  ): Either[String, String] =
    applyT((file: File) => fetchInfo(file, List(Path(Nil)), Nil))(
      templateDirectory,
      outputDirectory,
      arguments,
      forceOverwrite,
      None
    )

  val defaultTemplatePaths: List[Path] = List(path("src") / "main" / "g8", Path(Nil))

  def apply(fromMapping: Seq[(File, String)], toPath: File, parameters: Map[String, String]): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap { case (in, relative) =>
      expandPath(relative, toPath, parameters) match {
        case None      => Seq()
        case Some(out) => apply(in, out, toPath, parameters)
      }
    }

  def apply(in: File, out: File, base: File, parameters: Map[String, String]) = {
    try {
      if (verbatim(out, parameters, base)) FileUtils.copyFile(in, out)
      else {
        write(in, out, parameters /*, false*/ )
      }
    } catch {
      case e: Exception =>
        println("Falling back to file copy for %s: %s" format (in.toString, e.getMessage))
        FileUtils.copyFile(in, out)
    }
    allCatch opt {
      if (in.canExecute) out.setExecutable(true)
    }
    Seq(out)
  }

  def applyTemplate(default: String, resolved: ResolvedProperties): String = {
    val group = new STGroup('$', '$')
    group.setListener(new STErrorHandler)
    group.registerRenderer(
      classOf[AugmentedString],
      new AttributeRenderer[AugmentedString] {
        override def toString(o: AugmentedString, format: String, locale: Locale) =
          renderer.toString(o, format, locale)
      }
    )

    val attributes = resolved mapValues AugmentedString.apply

    try {
      val st = new ST(group, default)
      attributes.foreach { case (k, v) =>
        st.add(k, v)
      }
      st.render()
    } catch {
      case NonFatal(e: Exception) =>
        // This is a workaround for https://github.com/antlr/stringtemplate4/issues/253.
        throw new STException(
          "An unexpected error occurred while processing the template. " +
            "Check that all literal '$' are properly escaped with '\\$'",
          e
        )
    }
  }

  class STErrorHandler extends STErrorListener {
    def compileTimeError(msg: STMessage): Unit = {
      throw new STException(msg.toString, null)
    }
    def runTimeError(msg: STMessage): Unit = {
      throw new STException(msg.toString, null)
    }
    def IOError(msg: STMessage): Unit = {
      throw new STException(msg.toString, null)
    }
    def internalError(msg: STMessage): Unit = {
      throw new STException(msg.toString, null)
    }
  }

  /** The ValueF implementation for handling default properties. It performs formatted substitution on any properties
    * found.
    */
  case class DefaultValueF(default: String) extends ValueF {
    override def apply(resolved: ResolvedProperties): String = applyTemplate(default, resolved)
  }

  private val renderer = new AugmentedStringRenderer

  def write(in: File, out: File, parameters: Map[String, String] /*, append: Boolean*/ ): Unit =
    try {
      Option(PlexusIoResourceAttributeUtils.getFileAttributes(in)) match {
        case Some(attr) =>
          val mode = attr.getOctalMode
          write(out, FileUtils.readFileToString(in, "UTF-8"), parameters /*, append*/ )
          Try(ArchiveEntryUtils.chmod(out, mode))
        case None =>
          // PlexusIoResourceAttributes is not available for some OS'es such as windows
          write(out, FileUtils.readFileToString(in, "UTF-8"), parameters /*, append*/ )
      }
    } catch {
      case e: STException =>
        // add the current file to the exception for debugging purposes
        throw new STException(s"File: $in, ${e.getMessage}", null)
      case t: Throwable =>
        throw t
    }

  def write(out: File, template: String, parameters: Map[String, String] /*, append: Boolean = false*/ ): Unit =
    FileUtils.writeStringToFile(out, applyTemplate(template, parameters), StandardCharsets.UTF_8 /*, append*/ )

  def verbatim(file: File, parameters: Map[String, String], base: File = new File(".")): Boolean = {

    val rules = JGitIgnore(
      parameters
        .get("verbatim")
        .toSeq
        .flatMap(_.split(' ').toSeq)*
    )

    rules.isIgnored(file, base)
  }

  def expandPath(relative: String, toPath: File, parameters: Map[String, String]): Option[File] =
    try {
      val fileSeparator = File.separator
      val fileParams = Map(parameters.toSeq map {
        case (k, v) if k == "package" =>
          (k, v.replaceAll("""\.""", Matcher.quoteReplacement(fileSeparator)))
        case x => x
      }: _*)

      val ignored = relative
        .split("/")
        .map(part => applyTemplate(formatize(part), fileParams))
        .exists(_.isEmpty)

      if (ignored) None
      else Some(new File(toPath, applyTemplate(formatize(relative), fileParams)))
    } catch {
      case e: STException =>
        // add the current relative path to the exception for debugging purposes
        throw new STException(s"relative: $relative, toPath: $toPath, ${e.getMessage}", null)
      case t: Throwable =>
        throw t
    }

  private def formatize(s: String) = s.replaceAll("""\$(\w+)__([\w,]+)\$""", """\$$1;format="$2"\$""")

  def decapitalize(s: String)    = if (s.isEmpty) s else s(0).toLower + s.substring(1)
  def startCase(s: String)       = s.toLowerCase.split(" ").map(_.capitalize).mkString(" ")
  def wordOnly(s: String)        = s.replaceAll("""\W""", "")
  def space(s: String)           = s.replaceAll("""[\W_]+""", " ")
  def upperCamel(s: String)      = wordOnly(startCase(s))
  def lowerCamel(s: String)      = decapitalize(upperCamel(s))
  def hyphenate(s: String)       = s.replaceAll("""\s+""", "-")
  def normalize(s: String)       = hyphenate(s.toLowerCase)
  def snakeCase(s: String)       = s.replaceAll("""[\s\.\-]+""", "_")
  def packageNaming(s: String)   = s.replaceAll("""\s+""", ".")
  def packageDir(s: String)      = s.replace(".", System.getProperty("file.separator"))
  def dotReverse(s: String)      = s.split('.').reverse.mkString(".")
  def addRandomId(s: String)     = s + "-" + new java.math.BigInteger(256, new java.security.SecureRandom).toString(32)
  def scalaIdentifier(s: String) = if (s.contains("-")) "`" + s + "`" else s

  val Param = """(?s)^--(\S+)=(.+)$""".r
  implicit class RichFile(file: File) {
    def /(child: String): File = new File(file, child)
    def /(path: Path): File    = path.paths.foldLeft(file) { _ / _ }
  }
  def path(path: String): Path = Path(List(path))

  private[giter8] def getFiles(filter: File => Boolean)(f: File): Stream[File] =
    if (f.isDirectory) f.listFiles().toStream.withFilter(filter).flatMap(getFiles(filter))
    else if (filter(f)) Stream(f)
    else Stream()

  private[giter8] def getFiles(f: File): Stream[File] =
    getFiles(_ => true)(f)

  /** Select the root template directory from the given relative paths. */
  def templateRoot(baseDirectory: File, templatePaths: List[Path]): File =
    // Go through the list of dirs and pick the first one.
    (templatePaths map {
      case Path(Nil) => baseDirectory
      case p         => baseDirectory / p
    }).find(_.exists).getOrElse(baseDirectory)

  /** Extract template files under the first matching relative templatePaths under the baseDirectory. */
  def templateFiles(root: File, baseDirectory: File): Stream[File] = {

    val gitignoreFile: File = root / ".gitignore"
    val g8IgnoreFile: File  = baseDirectory / ".g8ignore"

    val g8Ignores: JGitIgnore = {

      lazy val defaults: JGitIgnore = JGitIgnore(
        ".git",
        "giter8.sbt",
        "project/giter8.sbt",
        "g8.sbt",
        "project/g8.sbt",
        "activator.properties",
        "project/activator.properties",
        "template.properties",
        "project/template.properties",
        "test",
        "!**/sbt-test/**/test",
        "!test/",
        "project/test",
        "!project/test/",
        "test.script",
        "project/test.script",
        "giter8.test",
        "project/giter8.test",
        "g8.test",
        "project/g8.test",
        "target/",
        "**/target/"
      )

      if (g8IgnoreFile.exists) {
        JGitIgnore(g8IgnoreFile)
      } else {
        defaults
      }
    }

    val gitIgnores: Option[JGitIgnore] =
      Some(gitignoreFile)
        .withFilter(_.exists)
        .map(JGitIgnore.apply)

    // The list of ignore rules. Rules in `.g8ignore` will
    // override those in `.gitignore`
    val ignores = gitIgnores.map(_ ++ g8Ignores).getOrElse(g8Ignores)

    def fileFilter(file: File): Boolean = {
      val isIgnored = ignores.isIgnored(file, root)
      !(isIgnored || file == g8IgnoreFile)
    }

    getFiles(fileFilter _)(root)
  }

  private[giter8] def applyT(
      fetch: File => Either[String, (UnresolvedProperties, Stream[File], File, Option[File])]
      // isScaffolding: Boolean = false
  )(
      tmpl: File,
      workingDirectory: File,
      arguments: Seq[String] = Nil,
      forceOverwrite: Boolean = false,
      outputDirectory: Option[File]
  ): Either[String, String] =
    try {
      fetch(tmpl).flatMap { case (defaults, templates, templatesRoot, scaffoldsRoot) =>
        val parameters = consoleParams(defaults, arguments).getOrElse {
          interact(defaults)
        }

        val base: File = outputDirectory
          .orElse {
            parameters.get("name").map(workingDirectory / G8.normalize(_))
          }
          .getOrElse(workingDirectory)

        val r = writeTemplates(
          templatesRoot,
          templates,
          parameters,
          base, // isScaffolding,
          forceOverwrite
        )
        for {
          _    <- r
          root <- scaffoldsRoot
        } copyScaffolds(root, base)
        r
      }
    } catch {
      case e: STException =>
        Left(s"Exiting due to error in the template: ${tmpl}\n${e.getMessage}")
      case NullInputException =>
        Left(s"Interrupting...")
      case t: Throwable =>
        t.printStackTrace()
        Left("Unknown exception: " + t.getMessage)
    }

  private def getVisibleFiles = getFiles(!_.isHidden) _

  /** transforms any maven() property operations to the latest version number reported by that service.
    */
  def transformProps(props: G8.OrderedProperties): Either[String, G8.OrderedProperties] =
    Maven.lookup(props)

  /** Extract params, template files, and scaffolding folder based on the conventionnal project structure
    */
  private[giter8] def fetchInfo(
      baseDirectory: File,
      templatePaths: List[Path],
      scaffoldPaths: List[Path]
  ): Either[String, (UnresolvedProperties, Stream[File], File, Option[File])] = {
    import java.io.FileInputStream
    val templatesRoot  = templateRoot(baseDirectory, templatePaths)
    val fs             = templateFiles(templatesRoot, baseDirectory)
    val metaDir        = baseDirectory / "project"
    val propertiesLoc0 = templatesRoot / "default.properties"
    val propertiesLoc1 = metaDir / "default.properties"
    val propertiesLocs = Set(propertiesLoc0, propertiesLoc1)
    val scaffoldsRoot =
      (scaffoldPaths map {
        case Path(Nil) => baseDirectory
        case p         => baseDirectory / p
      }).find(_.exists)
    val (propertiesFiles, tmpls) = fs.partition {
      propertiesLocs(_)
    }

    val parametersEither = propertiesFiles.headOption
      .map { f =>
        val props       = readProps(new FileInputStream(f))
        val transformed = transformProps(props)
        transformed.map { _.map { case (k, v) => (k, DefaultValueF(v)) } }
      }
      .getOrElse(Right(UnresolvedProperties.empty))

    val g8templates = tmpls.filter(!_.isDirectory)

    for (parameters <- parametersEither) yield (parameters, g8templates, templatesRoot, scaffoldsRoot)
  }

  def consoleParams(defaults: UnresolvedProperties, arguments: Seq[String]) = {
    arguments.headOption.map { _ =>
      val specified = arguments.foldLeft(ResolvedProperties.empty) {
        case (map, Param(key, value)) if defaults.map(_._1).contains(key) =>
          map + (key -> value)
        case (map, Param(key, _)) =>
          println("Ignoring unrecognized parameter: " + key)
          map
      }

      // Add anything from defaults that wasn't picked up as an argument from the console.
      defaults.foldLeft(specified) { case (resolved, (k, f)) =>
        if (!resolved.contains(k)) resolved + (k -> f(resolved))
        else resolved
      }
    }
  }

  def interact(params: UnresolvedProperties): ResolvedProperties = {
    val description = params
      .collectFirst {
        case (param, getDescription) if param == "description" =>
          getDescription(ResolvedProperties.empty)
      }

    description.foreach { descr =>
      descr.split("\n").foreach { line =>
        @scala.annotation.tailrec
        def liner(cursor: Int, rem: Iterable[String]): Unit = {
          if (rem.nonEmpty) {
            val next = cursor + 1 + rem.head.length
            if (next > 70 && cursor > 0) {
              println()
              liner(0, rem)
            } else {
              print(rem.head + " ")
              liner(next, rem.tail)
            }
          }
        }
        liner(0, line.split(" "))
        println()
      }
      println()
    }

    val fixed = Set("verbatim", "description")

    params
      .foldLeft(ResolvedProperties.empty) { case (resolved, (k, f)) =>
        resolved + (
          if (fixed.contains(k)) k -> f(resolved)
          else {
            val default = f(resolved)
            val message = Truthy.getMessage(default)

            print(s"$k [$message]: ")
            Console.flush() // Gotta flush for Windows console!
            val in = Option(scala.io.StdIn.readLine())
              .map(_.trim)
              .getOrElse(throw NullInputException)
            (k, if (in.isEmpty) default else in)
          }
        )
      }
      .toMap
  }

  case object NullInputException extends Throwable

  private def relativize(in: File, from: File): String = from.toURI().relativize(in.toURI).getPath

  def writeTemplates(
      tmpl: File,
      templates: Iterable[File],
      parameters: Map[String, String],
      base: File,
      // isScaffolding: Boolean,
      forceOverwrite: Boolean
  ): Either[String, String] = {

    import java.nio.charset.MalformedInputException

    templates
      .map { in =>
        val name      = relativize(in, tmpl)
        val optionOut = G8.expandPath(name, base, parameters)
        (in, optionOut)
      }
      .foreach { case (in, optionOut) =>
        optionOut match {
          case None => println("Skipping ignored file: %s" format in.toString)
          case Some(out) => {
            if (out.exists && !forceOverwrite) {
              println("Skipping existing file: %s" format out.toString)
            } else {
              out.getParentFile.mkdirs()
              if (G8.verbatim(in, parameters, tmpl))
                FileUtils.copyFile(in, out)
              else {
                catching(classOf[MalformedInputException])
                  .opt {
                    G8.write(in, out, parameters /*, append = existingScaffoldingAction.getOrElse(false)*/ )
                  }
                  .getOrElse {
                    // if (existingScaffoldingAction.getOrElse(false)) {
                    //   val existing = FileUtils.readFileToString(in, UTF_8)
                    //   FileUtils.write(out, existing, UTF_8, true)
                    // } else {
                    FileUtils.copyFile(in, out)
                    // }
                  }
              }
              if (in.canExecute) {
                out.setExecutable(true)
              }
            }
          }
        }
      }

    Right("Template applied in %s" format (base.toString))
  }

  def copyScaffolds(sf: File, output: File): Unit = {

    val scaffolds = if (sf.exists) Some(getFiles(_ => true)(sf)) else None

    for (
      fs <- scaffolds;
      f  <- fs if !f.isDirectory
    ) {
      // Copy scaffolding recipes
      val realProjectRoot = getVisibleFiles(output)
        .filter(_.isDirectory)
        .filter(_.getName == "project")
        .map(_.getParentFile)
        .headOption
        .getOrElse(output)

      val hidden = new File(realProjectRoot, ".g8")
      val name   = relativize(f, sf)
      val out    = new File(hidden, name)
      FileUtils.copyFile(f, out)
    }
  }

  def readProps(stm: InputStream): G8.OrderedProperties = {
    // Overrides java.util.Properties to return properties back in the order they were specified
    val parameters = mutable.ListBuffer.empty[(String, String)]
    val properties = new Properties {

      override def put(key: Object, value: Object) = {
        val k = key.toString
        val v = value.toString

        parameters += k -> v
        super.put(k, v)
      }
    }

    try {
      properties.load(stm)
      parameters.toList
    } finally {
      stm.close()
    }
  }
}

case class Path(paths: List[String]) {
  def /(child: String): Path = copy(paths = paths ::: List(child))
}

class AugmentedStringRenderer {
  import G8._
  def toString(value: String): String = value

  def toString(value: AugmentedString, formatName: String, locale: java.util.Locale): String = {
    val str = value.toString

    if (formatName == null) str
    else {
      val formats = formatName.split(",").map(_.trim)
      formats.foldLeft(str)(format)
    }
  }

  def format(value: String, formatName: String): String = formatName match {
    case "upper"      | "uppercase"       => value.toUpperCase
    case "lower"      | "lowercase"       => value.toLowerCase
    case "cap"        | "capitalize"      => value.capitalize
    case "decap"      | "decapitalize"    => decapitalize(value)
    case "start"      | "start-case"      => startCase(value)
    case "word"       | "word-only"       => wordOnly(value)
    case "space"      | "word-space"      => space(value)
    case "Camel"      | "upper-camel"     => upperCamel(value)
    case "camel"      | "lower-camel"     => lowerCamel(value)
    case "hyphen"     | "hyphenate"       => hyphenate(value)
    case "norm"       | "normalize"       => normalize(value)
    case "snake"      | "snake-case"      => snakeCase(value)
    case "package"    | "package-naming"  => packageNaming(value)
    case "packaged"   | "package-dir"     => packageDir(value)
    case "dotReverse" | "dot-reverse"     => dotReverse(value)
    case "random"     | "generate-random" => addRandomId(value)
    case _ => value
  }
}
