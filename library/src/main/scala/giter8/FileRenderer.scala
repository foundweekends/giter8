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

import scala.util.{Failure, Success, Try}

object FileRenderer {
  case class FileRenderingError(file: String, message: String) extends RuntimeException(s"File: $file, $message")

  def renderFile(in: File, out: File, parameters: Map[String, String]): Try[Unit] = {
    for {
      _   <- Try(out.getParentFile.mkdirs())
      _   <- copyOrRender(in, out, parameters)
      _   <- copyExecutableAttribute(in, out)
      res <- copyFileAttributes(in, out)
    } yield ()
  }

  private def copyOrRender(in: File, out: File, parameters: Map[String, String]): Try[Unit] = {
    if (verbatim(out, parameters)) Try(FileUtils.copyFile(in, out))
    else {
      renderFileImpl(in, out, parameters) recoverWith {
        //        case e: MalformedInputException             => Try(FileUtils.copyFile(in, out))
        case e: StringRenderer.StringRenderingError =>
          // add the current file to the exception for debugging purposes
          Failure(FileRenderingError(in.getAbsolutePath, e.getMessage))
      }
    }
  }

  private def copyExecutableAttribute(in: File, out: File): Try[Unit] = Try {
    if (in.canExecute) out.setExecutable(true)
  }

  private def renderFileImpl(in: File, out: File, parameters: Map[String, String]): Try[Unit] = {
    for {
      templateBody <- Try(FileUtils.readFileToString(in, "UTF-8"))
      content      <- StringRenderer.render(templateBody, parameters)
      res <- Try {
        FileUtils.writeStringToFile(out, content, UTF_8)
        copyFileAttributes(in, out)
      }
    } yield res
  }

  private def copyFileAttributes(in: File, out: File): Try[Unit] = Try {
    Option(PlexusIoResourceAttributeUtils.getFileAttributes(in)) match {
      case None       => // PlexusIoResourceAttributes is not available for some OS'es such as windows
      case Some(attr) => ArchiveEntryUtils.chmod(out, attr.getOctalMode, new ConsoleLogger(Logger.LEVEL_ERROR, ""))
    }
  }

  val verbatimKey = "verbatim"

  private[giter8] def verbatimFunction(propResolver: PropertyResolver): File => Boolean = {
    val verbatimValue: Option[String] = propResolver.resolve(Map.empty) match {
      case Failure(_) => None
      case Success(map) => map.get(verbatimKey)
    }
    { file: File =>
      verbatim(file, verbatimValue)
    }
  }

  private[giter8] def verbatim(file: File, parameters: Map[String, String]): Boolean =
    verbatim(file, parameters.get(verbatimKey))

  private[giter8] def verbatim(file: File, verbatimValue: Option[String]): Boolean =
    verbatimValue match {
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
