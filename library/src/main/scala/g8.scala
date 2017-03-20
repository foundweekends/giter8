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

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.stringtemplate.v4.compiler.STException

import scala.io.Source
import scala.util.{Failure, Success, Try}

object G8 {

  import FileDsl._

  // Called from JgitHelper
  def fromDirectory(baseDirectory: File,
                    outputDirectory: File,
                    arguments: Seq[String],
                    forceOverwrite: Boolean): Either[String, String] = {
    fromDir(
      baseDirectory,
      outputDirectory,
      arguments,
      forceOverwrite,
      defaultTemplatePaths,
      List(path("src") / "main" / "scaffolds", path("project") / "src" / "main" / "scaffolds")
    )
  }

  // Called from ScaffoldPlugin
  def fromDirectoryRaw(baseDirectory: File,
                       outputDirectory: File,
                       arguments: Seq[String],
                       forceOverwrite: Boolean): Either[String, String] = {
    fromDir(baseDirectory, outputDirectory, arguments, forceOverwrite, List(Path(Nil)), Nil)
  }

  def fromDir(baseDirectory: File,
              outputDirectory: File,
              arguments: Seq[String],
              forceOverwrite: Boolean,
              templatePaths: List[Path],
              scaffoldPaths: List[Path]): Either[String, String] = {
    try {

      val template = Template(baseDirectory, templatePaths, scaffoldPaths)

      val parametersTry = for {
        fromFiles       <- FilePropertyResolver(template.propertyFiles: _*).resolve(Map.empty)
        fromMaven       <- MavenPropertyResolver(httpClient).resolve(fromFiles)
        parsedArgs      <- Try(Util.parseArguments(arguments))
        fromArguments   <- StaticPropertyResolver(parsedArgs).resolve(fromMaven)
        fromInteraction <- InteractivePropertyResolver.resolve(fromArguments)
      } yield fromInteraction

      parametersTry match {
        case Failure(e) => Left(e.getMessage)
        case Success(parameters) =>
          val base = outputDirectory / parameters.get("name").map(FormatFunctions.normalize).getOrElse(".")
          val r = TemplateRenderer.render(template.root, template.templateFiles, base, parameters) match {
            case Success(s) => Right(s)
            case Failure(t) => Left(t.getMessage)
          }
          for {
            _    <- r.right
            root <- template.scaffoldsRoot
          } TemplateRenderer.copyScaffolds(root, base)
          r
      }
    } catch {
      case e: STException =>
        Left(s"Exiting due to error in the template\n${e.getMessage}")
      case t: Throwable =>
        Left("Unknown exception: " + t.getMessage)
    }
  }

  val defaultTemplatePaths: List[Path] = List(path("src") / "main" / "g8", Path(Nil))

  def apply(fromMapping: Seq[(File, String)], toPath: File, parameters: Map[String, String]): Seq[File] =
    fromMapping filter {
      !_._1.isDirectory
    } flatMap {
      case (in, relative) =>
        val out = TemplateRenderer.expandPath(relative, toPath, parameters).get
        FileRenderer.renderFile(in, out, parameters) match {
          case Success(()) => Seq(out)
          case Failure(t)  => throw t
        }
    }

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
}
