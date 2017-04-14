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

import java.io.File
import java.nio.charset.MalformedInputException
import java.util.logging.{Level, Logger}

import giter8.Template.files

import scala.io.Source
import scala.util.{Failure, Success, Try}

class Template private (baseDirectory: File, val root: File, val scaffoldsRoot: Option[File]) {
  import FileDsl._

  private val log = Logger.getLogger("giter8.Template")

  private val PropertyMatcher = """\$([^\;\$]*)(;format=\"[^\$\"]*\")?\$""".r

  private lazy val metaDir     = baseDirectory / "project"
  private lazy val gitFiles    = files(baseDirectory / ".git").toSet
  private lazy val pluginFiles = baseAndMeta("giter8.sbt", "g8.sbt")
  private lazy val metadata    = baseAndMeta("activator.properties", "template.properties")
  private lazy val testFiles   = baseAndMeta("test", "giter8.test", "g8.test")

  private lazy val possiblePropertyFiles = Set(
    root / "default.properties",
    baseDirectory / "project" / "default.properties"
  )

  private lazy val ignores: Option[JGitIgnore] = {
    val gitignoreFile: File = files(root / ".gitignore").head
    if (gitignoreFile.exists) Some(JGitIgnore(gitignoreFile)) else None
  }

  val scaffoldsFiles: Seq[File] = scaffoldsRoot.map(files).getOrElse(Seq.empty)

  lazy val (propertyFiles, templateFiles) = files(root)
    .filterNot(isIgnored)
    .filterNot(gitFiles)
    .filterNot(pluginFiles)
    .filterNot(metadata)
    .filterNot(testFiles)
    .filterNot(isTargetDir)
    .filterNot(_.isDirectory)
    .filterNot(scaffoldsFiles.toSet)
    .partition(possiblePropertyFiles)

  val properties: Set[String] = findProperties(templateFiles)

  private def findProperties(templateFiles: Stream[File]): Set[String] = templateFiles.flatMap(findProperties).toSet

  private def findProperties(file: File): Seq[String] = {
    Try(Source.fromFile(file).getLines.toSeq) match {
      case Failure(e: MalformedInputException) => Seq.empty
      case Failure(e) =>
        log.warning(s"Unable to read the file ${Util.relativePath(baseDirectory, file)}: ${e.getMessage}")
        Seq.empty
      case Success(lines) =>
        lines.flatMap { line =>
          PropertyMatcher.findAllMatchIn(line).map(_.group(1))
        }
    }
  }

  private def baseAndMeta(xs: String*): Set[File] = xs.toSet flatMap { x: String =>
    Set(baseDirectory / x, metaDir / x)
  }

  private def isIgnored(file: File): Boolean = {
    val p = file.toURI.toASCIIString
    ignores.isDefined && ignores.get.isIgnored(p)
  }

  private def isTargetDir(file: File): Boolean = {
    val p = file.toURI.toASCIIString
    p.stripPrefix(baseDirectory.toURI.toASCIIString).contains("target/")
  }
}

object Template {
  import FileDsl._

  val defaultTemplatePaths  = Seq(path("src") / "main" / "g8", Path(Nil))
  val defaultScaffoldsPaths = Seq(path("src") / "main" / "scaffolds", path("project") / "src" / "main" / "scaffolds")

  def apply(directory: File,
            templatePaths: Seq[Path]  = defaultTemplatePaths,
            scaffoldsPaths: Seq[Path] = defaultScaffoldsPaths): Template = {
    val templatesRoot = findTemplateRoot(directory, templatePaths)
    val scaffoldsRoot = findScaffoldsRoot(directory, scaffoldsPaths)
    new Template(directory, templatesRoot, scaffoldsRoot)
  }

  /** Select the root template directory from the given relative paths. */
  private def findTemplateRoot(baseDirectory: File, templatePaths: Seq[Path]): File = {
    // Go through the list of dirs and pick the first one
    val possibleRoots: Seq[File] = templatePaths map {
      case Path(Nil) => baseDirectory
      case p         => baseDirectory / p
    }
    possibleRoots.find(_.exists).getOrElse(baseDirectory)
  }

  private def findScaffoldsRoot(baseDirectory: File, scaffoldPaths: Seq[Path]): Option[File] = {
    val possibleRoots: Seq[File] = scaffoldPaths map {
      case Path(Nil) => baseDirectory
      case p         => baseDirectory / p
    }
    possibleRoots.find(_.exists)
  }

  private def files(base: File): Stream[File] = {
    if (base.isDirectory) base.listFiles.toStream.flatMap(files)
    else Stream(base)
  }
}
