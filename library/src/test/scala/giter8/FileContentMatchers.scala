package giter8

import java.io.File

import org.scalatest.matchers.{MatchResult, Matcher}

import scala.io.Source

trait FileContentMatchers {
  def haveContents(contents: String): Matcher[File] = new Matcher[File] {
    override def apply(left: File): MatchResult = {
      val fileContents = Source.fromFile(left).getLines().mkString("\n")
      MatchResult(
        matches = fileContents == contents,
        rawFailureMessage =
          s"File ${left.getAbsolutePath} has invalid contents:\n\texpected $contents\n\tactual: $fileContents",
        rawNegatedFailureMessage = s"${left.getAbsolutePath} has contents $contents"
      )
    }
  }
}
