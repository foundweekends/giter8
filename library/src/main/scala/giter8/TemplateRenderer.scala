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

import org.apache.commons.io.FileUtils
import org.stringtemplate.v4.compiler.STException

import scala.util.{Failure, Success, Try}

object TemplateRenderer {

  def render(templateRoot: File,
             templateFiles: Seq[File],
             outputDirectory: File,
             parameters: Map[String, String]): Try[String] = {

    templateFiles.foreach { file =>
      writeTemplateFile(templateRoot, file, parameters, outputDirectory)
    }
    Success(s"Template applied in ${outputDirectory.toString}")
  }

  def copyScaffolds(scaffoldsRoot: Option[File], scaffoldsFiles: Seq[File], outputDirectory: File): Try[Unit] = Try {
    scaffoldsRoot.foreach { root =>
      scaffoldsFiles.foreach { file =>
        val name = relativePath(root, file)
        val out  = new File(outputDirectory, name)
        FileUtils.copyFile(file, out)
      }
    }
  }

  private def writeTemplateFile(templateRoot: File, in: File, parameters: Map[String, String], base: File): Try[Unit] = {
    val relative = relativePath(templateRoot, in)
    expandPath(relative, base, parameters) flatMap { out =>
      if (out.exists) Success(println(s"Skipping existing file: ${out.toString}"))
      else FileRenderer.renderFile(in, out, parameters)
    } recoverWith {
      case e: STException =>
        // add the current relative path to the exception for debugging purposes
        Failure(new STException(s"relative: $relative, toPath: $base, ${e.getMessage}", null))
    }
  }

  private[giter8] def expandPath(relative: String, toPath: File, parameters: Map[String, String]): Try[File] = Try {
    val params = parameters map {
      case (k, v) if k == "package" =>
        val fileSeparator = System.getProperty("file.separator") match {
          case "\\" => "\\\\"
          case sep  => sep
        }
        k -> v.replace(".", fileSeparator)
      case x => x
    }

    new File(toPath, StringRenderer.render(FormatFunctions.formatize(relative), params).get)
  }

  private def relativePath(from: File, to: File): String = {
    val fromUri = from.toURI
    val toUti   = to.toURI
    fromUri.relativize(toUti).getPath
  }
}
