package giter8

import org.codehaus.plexus.components.io.attributes.PlexusIoResourceAttributeUtils
import org.scalatest.{FlatSpec, Matchers}

class FileRendererTest extends FlatSpec with Matchers {
  import TestFileHelpers._
  import FileDsl._
  import FileRenderer._

  "FileRenderer" should "render files" in tempDirectory { temp =>
    "$name$" >> (temp / "in")

    renderFile(temp / "in", temp / "out", Map("name" -> "foo"))

    temp / "out" should exist
    temp / "out" should haveContents("foo")
  }

  it should "copy files, marked as 'verbatim' unprocessed" in tempDirectory { temp =>
    "$name$" >> (temp / "in1")
    "$foo$" >> (temp / "in2.txt")

    val parameters = Map("verbatim" -> "out1 *.txt")
    renderFile(temp / "in1", temp / "out1", parameters)
    renderFile(temp / "in2.txt", temp / "out2.txt", parameters)

    temp / "out1" should exist
    temp / "out1" should haveContents("$name$")

    temp / "out2.txt" should exist
    temp / "out2.txt" should haveContents("$foo$")
  }

  it should "copy executable attribute" in tempDirectory { temp =>
    "foo" >> (temp / "in")
    (temp / "in").setExecutable(true)

    renderFile(temp / "in", temp / "out", Map.empty)

    temp / "out" should exist
    (temp / "out").canExecute shouldBe true
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
}
