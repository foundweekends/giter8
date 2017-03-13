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

import org.apache.commons.io.Charsets.UTF_8
import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.util.ArchiveEntryUtils
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger

import scala.util.{Failure, Try}

object FileRenderer {
  case class FileRenderingError(file: String, message: String) extends RuntimeException(s"File: $file, $message")

  def renderFile(in: File, out: File, parameters: Map[String, String]): Try[Unit] = Try {
    out.getParentFile.mkdirs()
    if (verbatim(out, parameters)) FileUtils.copyFile(in, out)
    else
      renderFileImpl(in, out, parameters) recoverWith {
        case e: StringRenderer.StringRenderingError =>
          // add the current file to the exception for debugging purposes
          Failure(FileRenderingError(in.getAbsolutePath, e.getMessage))
        case e: MalformedInputException => Try(FileUtils.copyFile(in, out))
      }
    copyExecutableAttribute(in, out)
  }

  private def copyExecutableAttribute(in: File, out: File) = if (in.canExecute) out.setExecutable(true)

  private def renderFileImpl(in: File, out: File, parameters: Map[String, String]): Try[Unit] = Try {
    val templateBody        = FileUtils.readFileToString(in, "UTF-8")
    val renderedFileContent = StringRenderer.render(templateBody, parameters).get
    FileUtils.writeStringToFile(out, renderedFileContent, UTF_8)
    copyFileAttributes(in, out)
  }

  private def copyFileAttributes(in: File, out: File): Unit =
    Option(PlexusIoResourceAttributeUtils.getFileAttributes(in)) match {
      case None       => // PlexusIoResourceAttributes is not available for some OS'es such as windows
      case Some(attr) => ArchiveEntryUtils.chmod(out, attr.getOctalMode, new ConsoleLogger(Logger.LEVEL_ERROR, ""))
    }

  private[giter8] def verbatim(file: File, parameters: Map[String, String]): Boolean =
    parameters.get("verbatim") match {
      case None => false
      case Some(patterns) =>
        patterns.split(' ').exists { pattern =>
          val translateRegex = pattern.flatMap {
            case '*' => """.*"""
            case '?' => """."""
            case '.' => """\."""
            case x   => x.toString
          }
          s"^$translateRegex$$".r.findFirstIn(file.getName).isDefined
        }
    }
}
