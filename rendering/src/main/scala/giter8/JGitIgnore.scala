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

import org.eclipse.jgit.ignore.FastIgnoreRule

import scala.io.Source

class JGitIgnore(patterns: Seq[String]) {
  def getPatterns: Seq[String] = patterns

  def isIgnored(path: String): Boolean = {
    val ignoreRules = patterns.map(new FastIgnoreRule(_))
    ignoreRules.exists { rule =>
      rule.getResult && rule.isMatch(path, false)
    }
  }
}

object JGitIgnore {
  def apply(patterns: Seq[String]): JGitIgnore = new JGitIgnore(patterns)

  def apply(in: InputStream): JGitIgnore = {
    val patterns = Source.fromInputStream(in).getLines().toIndexedSeq
    new JGitIgnore(patterns)
  }

  def apply(file: File): JGitIgnore = {
    val patterns = Source.fromFile(file).getLines().filterNot(_.startsWith("#")).filterNot(_.trim.isEmpty).toIndexedSeq
    new JGitIgnore(patterns)
  }
}
