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

import scala.io.Source
import scala.util.{Failure, Success, Try}

object TemplateRenderer {

  import Util.relativePath

  def render(templateRoot: File,
             templateFiles: Seq[File],
             outputDirectory: File,
             parameters: Map[String, String],
             force: Boolean): Try[Unit] = {

    for (file <- templateFiles) {
      val result = writeTemplateFile(templateRoot, file, parameters, outputDirectory, force)
      if (result.isFailure) return result
    }

    Success(())
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

  def showReadMe(path: File) = {
    if (!path.exists()) Success(())

    val readmeG8 = new File(path, "README.g8")
    Try {
      println()
      Source.fromFile(readmeG8, "UTF-8").getLines.foreach(println)
      println()
      readmeG8.delete()
    }
  }

  private def writeTemplateFile(templateRoot: File,
                                in: File,
                                parameters: Map[String, String],
                                base: File,
                                force: Boolean): Try[Unit] = {
    val relative = relativePath(templateRoot, in)
    expandPath(relative, base, parameters) flatMap { out =>
      if (out.exists && !force) Success(println(s"Skipping existing file: ${out.toString}"))
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
}
