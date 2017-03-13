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

import java.io.{File, FileInputStream}

import org.apache.commons.io.FileUtils
import org.stringtemplate.v4.compiler.STException

import scala.util.{Failure, Success}

object G8 {
  import FileDsl._

  /** Properties in the order they were created/defined */
  type OrderedProperties = List[(String, String)]
  object OrderedProperties {
    val empty = List.empty[(String, String)]
  }

  /** G8 template properties which have been fully resolved, i.e. defaults replaced by user input, ready for insertion into template */
  type ResolvedProperties = Map[String, String]
  object ResolvedProperties {
    val empty = Map.empty[String, String]
  }

  /**
    * A function which will return the resolved value of a property given the properties resolved thus far.
    * This is a bit more general than was needed for resolving "dynamic defaults". I did it this way so it's
    * possible to have other ValueF definitions which perform arbitrary logic given previously defined properties.
    */
  type ValueF = ResolvedProperties => String

  // Called from JgitHelper
  def fromDirectory(baseDirectory: File,
                    outputDirectory: File,
                    arguments: Seq[String],
                    forceOverwrite: Boolean): Either[String, String] =
    applyT(
      (file: File) =>
        fetchInfo(file: File,
                  defaultTemplatePaths,
                  List(path("src") / "main" / "scaffolds", path("project") / "src" / "main" / "scaffolds")))(
      baseDirectory,
      outputDirectory,
      arguments,
      forceOverwrite)

  // Called from ScaffoldPlugin
  def fromDirectoryRaw(baseDirectory: File,
                       outputDirectory: File,
                       arguments: Seq[String],
                       forceOverwrite: Boolean): Either[String, String] =
    applyT((file: File) => fetchInfo(file, List(Path(Nil)), Nil))(baseDirectory,
                                                                  outputDirectory,
                                                                  arguments,
                                                                  forceOverwrite)

  val defaultTemplatePaths: List[Path] = List(path("src") / "main" / "g8", Path(Nil))

  def apply(fromMapping: Seq[(File, String)], toPath: File, parameters: Map[String, String]): Seq[File] =
    fromMapping filter { !_._1.isDirectory } flatMap {
      case (in, relative) =>
        val out = expandPath(relative, toPath, parameters)
        FileRenderer.renderFile(in, out, parameters) match {
          case Success(()) => Seq(out)
          case Failure(t)  => throw t
        }
    }

  /** The ValueF implementation for handling default properties.  It performs formatted substitution on any properties found. */
  case class DefaultValueF(default: String) extends ValueF {
    override def apply(resolved: ResolvedProperties): String = StringRenderer.render(default, resolved).get
  }

  /** Properties which have not been resolved. I.e., ValueF() has not been evaluated */
  type UnresolvedProperties = List[(String, ValueF)]
  object UnresolvedProperties {
    val empty = List.empty[(String, ValueF)]
  }

  def expandPath(relative: String, toPath: File, parameters: Map[String, String]): File =
    try {
      val fileParams = Map(parameters.toSeq map {
        case (k, v) if k == "package" =>
          (k, v.replaceAll("""\.""", System.getProperty("file.separator") match {
            case "\\" => "\\\\"
            case sep  => sep
          }))
        case x => x
      }: _*)

      new File(toPath, StringRenderer.render(FormatFunctions.formatize(relative), fileParams).get)
    } catch {
      case e: STException =>
        // add the current relative path to the exception for debugging purposes
        throw new STException(s"relative: $relative, toPath: $toPath, ${e.getMessage}", null)
      case t: Throwable =>
        throw t
    }

  val Param = """^--(\S+)=(.+)$""".r

  private[giter8] def getFiles(filter: File => Boolean)(f: File): Stream[File] =
    if (f.isDirectory) f.listFiles().toStream.filter(filter).flatMap(getFiles(filter))
    else if (filter(f)) Stream(f)
    else Stream()

  private[giter8] def applyT(
      fetch: File => Either[String, (UnresolvedProperties, Stream[File], File, Option[File])]
      // isScaffolding: Boolean = false
  )(
      tmpl: File,
      outputFolder: File,
      arguments: Seq[String]  = Nil,
      forceOverwrite: Boolean = false
  ): Either[String, String] =
    try {
      fetch(tmpl).right.flatMap {
        case (defaults, templates, templatesRoot, scaffoldsRoot) =>
          val parameters = consoleParams(defaults, arguments).getOrElse {
            interact(defaults)
          }

          val base = outputFolder / parameters.get("name").map(FormatFunctions.normalize).getOrElse(".")
          val r = writeTemplates(templatesRoot,
                                 templates,
                                 parameters,
                                 base, // isScaffolding,
                                 forceOverwrite)
          for {
            _    <- r.right
            root <- scaffoldsRoot
          } copyScaffolds(root, base)
          r
      }
    } catch {
      case e: STException =>
        Left(s"Exiting due to error in the template\n${e.getMessage}")
      case t: Throwable =>
        Left("Unknown exception: " + t.getMessage)
    }

  private def getVisibleFiles = getFiles(!_.isHidden) _

  /** transforms any maven() property operations to the latest
    * version number reported by that service. */
  def transformProps(props: G8.OrderedProperties): Either[String, G8.OrderedProperties] =
    Maven.lookup(props)

  /**
    * Extract params, template files, and scaffolding folder based on the conventionnal project structure
    */
  private[giter8] def fetchInfo(
      baseDirectory: File,
      templatePaths: List[Path],
      scaffoldPaths: List[Path]): Either[String, (UnresolvedProperties, Stream[File], File, Option[File])] = {
    val template = Template(baseDirectory, templatePaths, scaffoldPaths)

    val parametersEither = template.propertyFiles.headOption
      .map { f =>
        val props       = readProps(new FileInputStream(f))
        val transformed = transformProps(props)
        transformed.right.map { _.map { case (k, v) => (k, DefaultValueF(v)) } }
      }
      .getOrElse(Right(UnresolvedProperties.empty))

    for (parameters <- parametersEither.right)
      yield (parameters, template.templateFiles.toStream, template.root, template.scaffoldsRoot)
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
      defaults.foldLeft(specified) {
        case (resolved, (k, f)) =>
          if (!resolved.contains(k)) resolved + (k -> f(resolved))
          else resolved
      }
    }
  }

  def interact(params: UnresolvedProperties): ResolvedProperties = {
    val (desc, others) = params partition { case (k, _) => k == "description" }

    desc.foreach { d =>
      @scala.annotation.tailrec
      def liner(cursor: Int, rem: Iterable[String]): Unit = {
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

    others
      .foldLeft(ResolvedProperties.empty) {
        case (resolved, (k, f)) =>
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
      }
      .toMap
  }

  private def relativize(in: File, from: File): String = from.toURI().relativize(in.toURI).getPath

  def writeTemplates(tmpl: File,
                     templates: Iterable[File],
                     parameters: Map[String, String],
                     base: File,
                     // isScaffolding: Boolean,
                     forceOverwrite: Boolean): Either[String, String] = {
    templates
      .map { in =>
        val name = relativize(in, tmpl)
        val out  = G8.expandPath(name, base, parameters)
        (in, out)
      }
      .foreach {
        case (in, out) =>
          if (out.exists) {
            println("Skipping existing file: %s" format out.toString)
          } else {
            FileRenderer.renderFile(in, out, parameters) match {
              case Success(()) => ()
              case Failure(e)  => return Left(e.getMessage)
            }
          }
      }

    Right("Template applied in %s" format (base.toString))
  }

  def copyScaffolds(sf: File, output: File): Unit = {

    val scaffolds = if (sf.exists) Some(getFiles(_ => true)(sf)) else None

    for (fs <- scaffolds;
         f  <- fs if !f.isDirectory) {
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

  def readProps(stm: java.io.InputStream): G8.OrderedProperties = {
    val p = new LinkedListProperties
    p.load(stm)
    stm.close()
    (OrderedProperties.empty /: p.keyList) { (l, k) =>
      l :+ (k -> p.getProperty(k))
    }
  }
}

/** Hacked override of java.util.Properties for the sake of getting the properties in the order they are specified in the file */
private[giter8] class LinkedListProperties extends java.util.Properties {
  var keyList = List.empty[String]

  override def put(k: Object, v: Object) = {
    keyList = keyList :+ k.toString
    super.put(k, v)
  }
}
