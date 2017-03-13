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
import java.net.URI

import org.apache.commons.io.FileUtils
import org.apache.http.{Header, HeaderIterator, ProtocolVersion, RequestLine}
import org.apache.http.client.methods.{HttpGet, HttpUriRequest}
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpParams
import org.stringtemplate.v4.compiler.STException

import scala.io.Source
import scala.util.{Failure, Success, Try}

object G8 {

  import FileDsl._

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
    fromMapping filter {
      !_._1.isDirectory
    } flatMap {
      case (in, relative) =>
        val out = expandPath(relative, toPath, parameters)
        FileRenderer.renderFile(in, out, parameters) match {
          case Success(()) => Seq(out)
          case Failure(t)  => throw t
        }
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
      fetch: File => Either[String, (Map[String, String], Stream[File], File, Option[File])]
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
          val parametersTry = for {
            parsedArgs      <- Try(Util.parseArguments(arguments))
            fromArguments   <- StaticPropertyResolver(parsedArgs).resolve(defaults)
            fromInteraction <- InteractivePropertyResolver.resolve(fromArguments)
          } yield fromInteraction

          parametersTry match {
            case Failure(e) => Left(e.getMessage)
            case Success(parameters) =>
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
      }
    } catch {
      case e: STException =>
        Left(s"Exiting due to error in the template\n${e.getMessage}")
      case t: Throwable =>
        Left("Unknown exception: " + t.getMessage)
    }

  private def getVisibleFiles = getFiles(!_.isHidden) _

  val httpClient = new HttpClient {
    val apacheHttpClient = new DefaultHttpClient
    override def execute(request: HttpGetRequest): Try[HttpResponse] = {
      Try(apacheHttpClient.execute(new HttpGet(request.url))).map { response =>
        val statusLine = response.getStatusLine
        val body = Option(response.getEntity).map { entity =>
          Source
            .fromInputStream(entity.getContent)
            .getLines()
            .mkString("\n")
        }

        HttpResponse(statusLine.getStatusCode, statusLine.getReasonPhrase, body)
      }
    }
  }

  /**
    * Extract params, template files, and scaffolding folder based on the conventionnal project structure
    */
  private[giter8] def fetchInfo(
      baseDirectory: File,
      templatePaths: List[Path],
      scaffoldPaths: List[Path]): Either[String, (Map[String, String], Stream[File], File, Option[File])] = {
    val template = Template(baseDirectory, templatePaths, scaffoldPaths)

    val parametersEither: Either[String, Map[String, String]] = template.propertyFiles.headOption
      .map { f =>
        FilePropertyResolver(f)
          .resolve(Map.empty)
          .flatMap(MavenPropertyResolver(httpClient).resolve) match {
          case Success(parameters) => Right(parameters)
          case Failure(e)          => Left(e.getMessage)
        }
      //        transformed.right.map { _.map { case (k, v) => (k, DefaultValueF(v)) } }
      }
      .getOrElse(Right(Map.empty))

    for (parameters <- parametersEither.right)
      yield (parameters, template.templateFiles, template.root, template.scaffoldsRoot)
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
}
