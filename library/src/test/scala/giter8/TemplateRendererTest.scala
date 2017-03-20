package giter8

import org.scalatest.{FlatSpec, Matchers}

class TemplateRendererTest extends FlatSpec with Matchers with TestFileHelpers with FileContentMatchers {
  import TemplateRenderer._
  import FileDsl._

  "TemplateRenderer" should "render template" in tempDirectory { temp =>
    "$foo$" >> (temp / "foo.txt")

    render(temp, Seq(temp / "foo.txt"), temp / "out", Map("foo" -> "bar"))

    temp / "out" / "foo.txt" should exist
    temp / "out" / "foo.txt" should haveContents("bar")
  }

  it should "resolve relative path" in tempDirectory { temp =>
    "$foo$" >> (temp / "A" / "foo.txt")
    "$baz$" >> (temp / "B" / "baz.txt")

    val templateFiles = Seq(temp / "A" / "foo.txt", temp / "B" / "baz.txt")
    render(temp, templateFiles, temp / "out", Map("foo" -> "bar", "baz" -> "quux"))

    temp / "out" / "A" / "foo.txt" should exist
    temp / "out" / "A" / "foo.txt" should haveContents("bar")

    temp / "out" / "B" / "baz.txt" should exist
    temp / "out" / "B" / "baz.txt" should haveContents("quux")
  }

  it should "expand package name" in tempDirectory { temp =>
    "$foo$" >> (temp / "$package$" / "foo.txt")

    render(temp, Seq(temp / "$package$" / "foo.txt"), temp / "out", Map("foo" -> "bar", "package" -> "com.example"))

    temp / "out" / "com" / "example" / "foo.txt" should exist
    temp / "out" / "com" / "example" / "foo.txt" should haveContents("bar")
  }
}
