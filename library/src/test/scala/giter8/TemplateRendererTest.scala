package giter8

import giter8.FileRenderer.FileRenderingError
import org.scalatest.{FlatSpec, Matchers, TryValues}

class TemplateRendererTest
    extends FlatSpec
    with Matchers
    with TestFileHelpers
    with FileContentMatchers
    with TryValues {
  import TemplateRenderer._
  import FileDsl._

  "TemplateRenderer" should "render template" in tempDirectory { temp =>
    "$foo$" >> (temp / "foo.txt")

    render(temp, Seq(temp / "foo.txt"), temp / "out", Map("foo" -> "bar"), force = false).success

    temp / "out" / "foo.txt" should exist
    temp / "out" / "foo.txt" should haveContents("bar")
  }

  it should "resolve relative path" in tempDirectory { temp =>
    "$foo$" >> (temp / "A" / "foo.txt")
    "$baz$" >> (temp / "B" / "baz.txt")

    val templateFiles = Seq(temp / "A" / "foo.txt", temp / "B" / "baz.txt")
    render(temp, templateFiles, temp / "out", Map("foo" -> "bar", "baz" -> "quux"), force = false).success

    temp / "out" / "A" / "foo.txt" should exist
    temp / "out" / "A" / "foo.txt" should haveContents("bar")

    temp / "out" / "B" / "baz.txt" should exist
    temp / "out" / "B" / "baz.txt" should haveContents("quux")
  }

  it should "expand package name" in tempDirectory { temp =>
    "$foo$" >> (temp / "$package$" / "foo.txt")

    render(temp,
           Seq(temp / "$package$" / "foo.txt"),
           temp / "out",
           Map("foo" -> "bar", "package" -> "com.example"),
           force = false).success

    temp / "out" / "com" / "example" / "foo.txt" should exist
    temp / "out" / "com" / "example" / "foo.txt" should haveContents("bar")
  }

  it should "copy scaffolds" in tempDirectory { temp =>
    touch(temp / "scaffolds" / "A" / "a.txt")
    touch(temp / "scaffolds" / "B" / "b.txt")

    val scaffoldsFiles = Seq(temp / "scaffolds" / "A" / "a.txt", temp / "scaffolds" / "B" / "b.txt")

    copyScaffolds(Some(temp / "scaffolds"), scaffoldsFiles, temp / "out" / ".g8").success

    temp / "out" / ".g8" / "A" / "a.txt" should exist
    temp / "out" / ".g8" / "B" / "b.txt" should exist
  }

  it should "preserve old files if force flag is false" in tempDirectory { temp =>
    "$foo$" >> (temp / "foo.txt")
    "old content" >> (temp / "out" / "foo.txt")

    render(temp, Seq(temp / "foo.txt"), temp / "out", Map("foo" -> "bar"), force = false).success

    temp / "out" / "foo.txt" should exist
    temp / "out" / "foo.txt" should haveContents("old content")
  }

  it should "override files if force flag is true" in tempDirectory { temp =>
    "$foo$" >> (temp / "foo.txt")
    "old content" >> (temp / "out" / "foo.txt")

    render(temp, Seq(temp / "foo.txt"), temp / "out", Map("foo" -> "bar"), force = true).success

    temp / "out" / "foo.txt" should exist
    temp / "out" / "foo.txt" should haveContents("bar")
  }

  it should "return an error if there are unspecified values" in tempDirectory { temp =>
    "$foo$" >> (temp / "foo.txt")

    val failure = render(temp, Seq(temp / "foo.txt"), temp / "out", Map.empty, force = false).failure
    failure.exception shouldBe a[FileRenderingError]
  }
}
