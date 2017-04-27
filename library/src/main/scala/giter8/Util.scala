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
import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter

import scala.collection.JavaConverters._
import scala.util.Try

object Util {

  def parseArguments(args: Seq[String]): Map[String, String] = {
    val param = """--(.*)=(.*)""".r
    val pairs = args.map {
      case param(k, v) => k -> v
    }
    pairs.toMap
  }

  def relativePath(from: File, to: File): String = {
    val fromUri = from.toURI
    val toUti   = to.toURI
    fromUri.relativize(toUti).getPath
  }

  def listFilesAndDirsWithSymbolicLinks(folder: File): Seq[File] = {
    val files = FileUtils.iterateFilesAndDirs(folder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).asScala.toSeq
    listFilesAndDirsWithSymbolicLinks(folder, files)
  }

  def listFilesAndDirsWithSymbolicLinks(folder: File, files: Seq[File]): Seq[File] = {
    def nonSymbolicFirst(a: File, b: File) = isSymbolicLink(a) < isSymbolicLink(b)
    files.sortWith(nonSymbolicFirst)
  }

  def hasSymbolicParent(f: File, topFolder: File): Boolean = {
    if (f.equals(topFolder)) false
    else if (isSymbolicLink(f)) true
    else hasSymbolicParent(f.getParentFile, topFolder)
  }

  def isSymbolicLink(file: File): Boolean = Files.isSymbolicLink(file.toPath)

  def link(in: File, out: File) = Try {
    val inTargetPath = Files.readSymbolicLink(in.toPath)
    val outTarget = new File(out.getParentFile.getAbsolutePath, inTargetPath.toFile.getName)
    Files.createSymbolicLink(out.toPath, outTarget.toPath)
    ()
  }
}
