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

import scala.io.Source
import scala.util.{Success, Try}

object Giter8 {
  import FileDsl._

  val defaultHttpClient = new HttpClient {
    val apacheHttpClient = new DefaultHttpClient
    override def execute(request: HttpGetRequest): Try[HttpResponse] = {
      Try(apacheHttpClient.execute(new HttpGet(request.url))).map { response =>
        val statusLine = response.getStatusLine
        val body = Option(response.getEntity).map { entity =>
          Source.fromInputStream(entity.getContent).getLines().mkString("\n")
        }
        HttpResponse(statusLine.getStatusCode, statusLine.getReasonPhrase, body)
      }
    }
  }

  def applyTemplate(templateDirectory: File,
                    templatePath: Option[String],
                    outputDirectory: File,
                    additionalProperties: Map[String, String],
                    interactive: Boolean = false): Try[String] =
    for {
      templateDirectory <- Try(new File(templateDirectory, templatePath.getOrElse("")))
      template          <- Try(Template(templateDirectory))
      propertyResolver  <- makePropertyResolver(template.propertyFiles, additionalProperties, interactive)
      parameters        <- propertyResolver.resolve(Map.empty)
      packageDir        <- Success(parameters.get("name").map(FormatFunctions.normalize).getOrElse("."))
      res               <- TemplateRenderer.render(template.root, template.templateFiles, outputDirectory / packageDir, parameters)
      _                 <- TemplateRenderer.copyScaffolds(template.scaffoldsRoot, template.scaffoldsFiles, outputDirectory / ".g8")
    } yield res

  private def makePropertyResolver(propertyFiles: Seq[File],
                                   additionalProperties: Map[String, String],
                                   interactive: Boolean) = Success {
    val resolvers = Seq(FilePropertyResolver(propertyFiles: _*),
                        MavenPropertyResolver(defaultHttpClient),
                        StaticPropertyResolver(additionalProperties))
    if (interactive) PropertyResolverChain(resolvers :+ InteractivePropertyResolver: _*)
    else PropertyResolverChain(resolvers: _*)
  }
}
