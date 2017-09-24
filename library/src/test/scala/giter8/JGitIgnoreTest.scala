package giter8

import java.io.ByteArrayInputStream
import java.net.URI

import org.scalatest.{FlatSpec, Matchers}

import scala.language.implicitConversions

class JGitIgnoreTest extends FlatSpec with Matchers {

  implicit def toURI(s: String): URI = new URI(s)

  "JGitIgnore" can "be created from Seq of string patterns" in {
    val patterns = Seq(".test")
    JGitIgnore(patterns: _*).getPatterns should contain theSameElementsAs patterns
  }

  it can "be created from InputStream" in {
    val patterns = Seq(".test")
    val stream   = new ByteArrayInputStream(patterns.mkString("\n").getBytes())
    JGitIgnore(stream).getPatterns should contain theSameElementsAs patterns
  }

  it should "check if file is ignored" in {
    val ignore = JGitIgnore("iAmIgnored")
    ignore.isIgnored("iAmNotIgnored") shouldBe false
    ignore.isIgnored("iAmIgnored") shouldBe true
  }

  it should "support wildcards" in {
    val ignore = JGitIgnore("*.test")
    ignore.isIgnored("foo.test") shouldBe true
    ignore.isIgnored("bar.test") shouldBe true
  }

  it should "support nested directories" in {
    val ignore = JGitIgnore("*.test")
    ignore.isIgnored("foo/foo.test") shouldBe true
    ignore.isIgnored("bar/bar.test") shouldBe true
  }

  it should "support negation" in {
    val ignore = JGitIgnore("*", "!foo")
    ignore.isIgnored("foo") shouldBe false
    ignore.isIgnored("bar") shouldBe true
  }

  it should "support precedence" in {
    val ignore = JGitIgnore("!foo", "*")
    ignore.isIgnored("foo") shouldBe true
    ignore.isIgnored("bar") shouldBe true
  }

  it should "support relativised files" in {
    val ignore = JGitIgnore("foo/")
    ignore.isIgnored("foo/bar") shouldBe true
    ignore.isIgnored("foo/bar", isDir = false, Some("foo")) shouldBe false
    ignore.isIgnored("foo/bar", isDir = true, Some("foo")) shouldBe false
  }
}
