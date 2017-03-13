package giter8

import org.scalatest.{FlatSpec, Matchers}

class TemplateTest extends FlatSpec with Matchers with TestFileHelpers {
  import FileDsl._

  "Giter8Template" should "treat src/main/g8 directory as template root if it exists" in tempDirectory { temp =>
    mkdir(temp / "template" / "src" / "main" / "g8")

    val template = Template(temp / "template")
    template.root should equal(temp / "template" / "src" / "main" / "g8")
  }

  it should "treat template directory as template root otherwise" in tempDirectory { temp =>
    mkdir(temp / "template")

    val template = Template(temp / "template")
    template.root should equal(temp / "template")
  }

  it should "treat files in root as template files" in tempDirectory { temp =>
    mkdir(temp / "template")
    "foo" >> (temp / "template" / "foo.txt")

    val template = Template(temp / "template")
    template.templateFiles should contain theSameElementsAs Seq(temp / "template" / "foo.txt")
  }

  it should "ignore g8.sbt and giter8.sbt files" in tempDirectory { temp =>
    mkdir(temp / "template")
    "I am template" >> (temp / "template" / "foo.txt")
    "I am plugin file" >> (temp / "template" / "g8.sbt")
    "I am plugin file" >> (temp / "template" / "giter8.sbt")
    "I am plugin file" >> (temp / "template" / "project" / "g8.sbt")
    "I am plugin file" >> (temp / "template" / "project" / "giter8.sbt")

    val template = Template(temp / "template")
    template.templateFiles should contain theSameElementsAs Seq(
      temp / "template" / "foo.txt"
    )
  }

  it should "ignore activator.properties and template.properties files" in tempDirectory { temp =>
    mkdir(temp / "template")
    "I am template" >> (temp / "template" / "foo.txt")
    "I am metadata file" >> (temp / "template" / "activator.properties")
    "I am metadata file" >> (temp / "template" / "template.properties")
    "I am metadata file" >> (temp / "template" / "project" / "activator.properties")
    "I am metadata file" >> (temp / "template" / "project" / "template.properties")

    val template = Template(temp / "template")
    template.templateFiles should contain theSameElementsAs Seq(
      temp / "template" / "foo.txt"
    )
  }
}
