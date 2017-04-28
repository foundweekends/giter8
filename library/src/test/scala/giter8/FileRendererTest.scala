package giter8

import giter8.TemplateRenderer.render
import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils
import org.scalatest.{FlatSpec, Matchers, TryValues}

class FileRendererTest extends FlatSpec with Matchers with TestFileHelpers with FileContentMatchers with TryValues {
  import FileDsl._
  import FileRenderer._

  "FileRenderer" should "render files" in tempDirectory { temp =>
    "$name$" >> (temp / "in")

    renderFile(temp / "in", temp / "out", Map("name" -> "foo")).success

    temp / "out" should exist
    temp / "out" should haveContents("foo")
  }

  it should "copy files, marked as 'verbatim' unprocessed" in tempDirectory { temp =>
    "$name$" >> (temp / "in1")
    "$foo$" >> (temp / "in2.txt")

    val parameters = Map("verbatim" -> "out1 *.txt")
    renderFile(temp / "in1", temp / "out1", parameters).success
    renderFile(temp / "in2.txt", temp / "out2.txt", parameters).success

    temp / "out1" should exist
    temp / "out1" should haveContents("$name$")

    temp / "out2.txt" should exist
    temp / "out2.txt" should haveContents("$foo$")
  }

  it should "copy executable attribute" in tempDirectory { temp =>
    "foo" >> (temp / "in")
    (temp / "in").setExecutable(true)

    renderFile(temp / "in", temp / "out", Map.empty).success

    temp / "out" should exist
    (temp / "out").canExecute shouldBe true
  }

  it should "return an error if" in tempDirectory { temp =>
    "$foo$" >> (temp / "foo.txt")

    val failure = renderFile(temp / "foo.txt", temp / "out" / "foo.txt", Map.empty).failure
    failure.exception shouldBe a[FileRenderingError]
  }

  it should "copy file attributes" in tempDirectory { temp =>
    "foo" >> (temp / "in")
    renderFile(temp / "in", temp / "out", Map.empty)
    temp / "out" should exist

    val expected = Option(PlexusIoResourceAttributeUtils.getFileAttributes(temp / "in"))
    val actual   = Option(PlexusIoResourceAttributeUtils.getFileAttributes(temp / "out"))

    if (actual.isDefined) {
      expected.isDefined shouldBe true
      actual.get.getGroupName should equal(expected.get.getGroupName)
      actual.get.getOctalMode should equal(expected.get.getOctalMode)
      actual.get.getUserName should equal(expected.get.getUserName)
      actual.get.isSymbolicLink should equal(expected.get.isSymbolicLink)
    } else expected.isDefined shouldBe false
  }

  it should "handle symbolic links" in tempDirectory { temp =>
    "$name$" >> (temp / "template" / "in")
    symLink(temp / "template" / "in_link", temp / "template" / "in")

    renderFile(temp / "template" / "in", temp / "output" / "in", Map("name" -> "foo")).success
    renderFile(temp / "template" / "in_link", temp / "output" / "in_link", Map("name" -> "foo")).success

    temp / "output" / "in" should exist
    temp / "output" / "in" should haveContents("foo")
    temp / "output" / "in" shouldNot beSymbolicLink()
    temp / "output" / "in_link" should beSymbolicLink()
  }
}
