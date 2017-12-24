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

import java.io.{File, InputStream}
import java.net.URI

import org.eclipse.jgit.ignore.FastIgnoreRule

import scala.io.Source

case class JGitIgnore(patterns: String*) {

  private lazy val ignoreRules: Seq[FastIgnoreRule] =
    patterns.map(new FastIgnoreRule(_))

  def getPatterns: Seq[String] = patterns

  /**
    * This method uses a manual precedence implementation for rules.
    *
    * The last rule to match a path will be used to determine if it is ignored.
    *
    * It is recommended to pass a `relativeTo` file to avoid matching against
    * absolute paths.
    *
    * @param uri
    * @param isDir
    * @param relativeTo a file which the subject path with be resolved relative to.
    * @return whether or not the file should be ignored
    */
  def isIgnored(uri: URI, isDir: Boolean = false, relativeTo: Option[URI] = None): Boolean = {

    val path = relativeTo
      .map {
        _.relativize(uri)
      }
      .getOrElse(uri)
      .getPath

    ignoreRules.filter(_.isMatch(path, isDir)).lastOption.exists(_.getResult)
  }

  def isIgnored(file: File, relativeTo: File): Boolean =
    isIgnored(file.toURI, file.isDirectory, Some(relativeTo.toURI))

  def ++(other: JGitIgnore): JGitIgnore =
    JGitIgnore(patterns ++ other.patterns: _*)
}

object JGitIgnore {

  def apply(in: InputStream): JGitIgnore = {
    val patterns = Source.fromInputStream(in).getLines().toIndexedSeq
    new JGitIgnore(patterns: _*)
  }

  def apply(file: File): JGitIgnore = {
    val patterns = Source.fromFile(file).getLines.filterNot(_.startsWith("#")).filterNot(_.trim.isEmpty).toIndexedSeq
    JGitIgnore(patterns: _*)
  }

  def fromFiles(files: File*): JGitIgnore = {
    val patterns: IndexedSeq[String] = files.foldLeft[IndexedSeq[String]](IndexedSeq.empty) {
      case (m, file) =>
        m ++ Source.fromFile(file).getLines().filterNot(_.startsWith("#")).filterNot(_.trim.isEmpty).toIndexedSeq
    }
    JGitIgnore(patterns: _*)
  }
}
