package giter8

import java.io.ByteArrayInputStream

import org.scalatest.{FlatSpec, Matchers}

class JGitIgnoreTest extends FlatSpec with Matchers {
  "JGitIgnore" can "be created from Seq of string patterns" in {
    val patterns = Seq(".test")
    JGitIgnore(patterns).getPatterns should contain theSameElementsAs patterns
  }

  it can "be created from InputStream" in {
    val patterns = Seq(".test")
    val stream   = new ByteArrayInputStream(patterns.mkString("\n").getBytes())
    JGitIgnore(stream).getPatterns should contain theSameElementsAs patterns
  }

  it should "check if file is ignored" in {
    val ignore = JGitIgnore(Seq("iAmIgnored"))
    ignore.isIgnored("notIgnored") shouldBe false
    ignore.isIgnored("iAmIgnored") shouldBe true
  }

  it should "support wildcards" in {
    val ignore = JGitIgnore(Seq("*.test"))
    ignore.isIgnored("foo.test") shouldBe true
    ignore.isIgnored("bar.test") shouldBe true
  }

  it should "support nested directories" in {
    val ignore = JGitIgnore(Seq("*.test"))
    ignore.isIgnored("foo/foo.test") shouldBe true
    ignore.isIgnored("bar/bar.test") shouldBe true
  }
}
