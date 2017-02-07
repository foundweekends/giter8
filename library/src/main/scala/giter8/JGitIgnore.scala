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
    val patterns = Source.fromFile(file).
      getLines().
      filterNot(_.startsWith("#")).
      filterNot(_.trim.isEmpty).
      toIndexedSeq
    new JGitIgnore(patterns)
  }
}
