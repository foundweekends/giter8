package giter8

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import G8._

class IntegrationTest extends FlatSpec with IntegrationTestHelpers with Matchers {
  import TestFileHelpers._

  "Giter8" should "treat sources root as template root" in testCase {
    case (template, expected, actual) =>
      "I am foo.txt" >> (template / "foo.txt")
      "I am foo.txt" >> (expected / "foo.txt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "treat src/main/g8 as template root" in testCase {
    case (template, expected, actual) =>
      "I am foo.txt" >> (template / "src" / "main" / "g8" / "foo.txt")
      "I am foo.txt" >> (expected / "foo.txt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "prefer src/main/g8 over sources root" in testCase {
    case (template, expected, actual) =>
      "I am foo.txt" >> (template / "foo.txt")
      "I am bar.txt" >> (template / "src" / "main" / "g8" / "bar.txt")
      "I am bar.txt" >> (expected / "bar.txt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "ignore empty directories" in testCase {
    case (template, expected, actual) =>
      mkdir(template / "empty")
      mkdir(template / "src" / "main" / "g8" / "empty")
      checkGeneratedProject(template, expected, actual)
  }

  it should "ignore .git folder" in testCase {
    case (template, expected, actual) =>
      touch(template / ".git" / "ignoreMe")
      checkGeneratedProject(template, expected, actual)
  }

  it should "ignore plugin files" in testCase {
    case (template, expected, actual) =>
      touch(template / "giter8.sbt")
      touch(template / "g8.sbt")
      touch(template / "project" / "giter8.sbt")
      touch(template / "project" / "g8.sbt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "ignore metadata files" in testCase {
    case (template, expected, actual) =>
      touch(template / "activator.properties")
      touch(template / "template.properties")
      touch(template / "project" / "activator.properties")
      touch(template / "project" / "template.properties")
      checkGeneratedProject(template, expected, actual)
  }

  it should "ignore test files" in testCase {
    case (template, expected, actual) =>
      touch(template / "test")
      touch(template / "giter8.test")
      touch(template / "g8.test")
      touch(template / "project" / "test")
      touch(template / "project" / "giter8.test")
      touch(template / "project" / "g8.test")
      checkGeneratedProject(template, expected, actual)
  }

  it should "ignore 'target' directories" in testCase {
    case (template, expected, actual) =>
      touch(template / "target" / "ignoreMe")
      touch(template / "project" / "target" / "ignoreMe")
      touch(template / "src" / "main" / "g8" / "target" / "ignoreMe")
      checkGeneratedProject(template, expected, actual)
  }

  it should "read default.properties from template root" in testCase {
    case (template, expected, actual) =>
      "foo = bar" >> (template / "src" / "main" / "g8" / "default.properties")
      "$foo$" >> (template / "src" / "main" / "g8" / "foo.txt")
      "bar" >> (expected / "foo.txt")
      checkGeneratedProject(template, expected, actual)
  }

  ignore should "read default.properties from sbt project directory" in testCase {
    case (template, expected, actual) =>
      "foo = bar" >> (template / "project" / "default.properties")
      "$foo$" >> (template / "src" / "main" / "g8" / "foo.txt")
      "bar" >> (expected / "foo.txt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "handle booleans" in testCase {
    case (template, expected, actual) =>
      """foo = now you
        |show = yes
        |hide = false
      """.stripMargin >> (template / "src" / "main" / "g8" / "default.properties")
      "$show$, $foo$ $if(show.truthy)$see me$endif$$if(hide.truthy)$don't$endif$" >> (template / "src" / "main" / "g8" / "foo.txt")
      "yes, now you see me" >> (expected / "foo.txt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "create directory with project name" in testCase {
    case (template, expected, actual) =>
      "name = My awesome Project" >> (template / "src" / "main" / "g8" / "default.properties")
      "This is $name$" >> (template / "src" / "main" / "g8" / "foo.txt")
      "This is My awesome Project" >> (expected / "my-awesome-project" / "foo.txt")
      checkGeneratedProject(template, expected, actual)
  }

  it should "resolve package names" in testCase {
    case (template, expected, actual) =>
      """|name = Package test
       |package = com.example
    """.stripMargin >> (template / "src" / "main" / "g8" / "default.properties")

      "package $package$" >> (template / "src/main/g8/src/main/scala" / "$package$" / "Main.scala")
      "package com.example" >> (expected / "package-test" / "src/main/scala" / "com/example" / "Main.scala")
      checkGeneratedProject(template, expected, actual)
  }

  it should "respect .gitignore in template root" in testCase {
    case (template, expected, actual) =>
      val gitignore =
        """|foo.txt
         |*.test
         |.idea/
         |.DS_Store
      """.stripMargin
      gitignore >> (template / "src" / "main" / "g8" / ".gitignore")
      gitignore >> (expected / ".gitignore")

      touch(template / "src" / "main" / "g8" / "foo.txt")
      touch(template / "src" / "main" / "g8" / "bar.test")
      touch(template / "src" / "main" / "g8" / ".DS_Store")
      touch(template / "src" / "main" / "g8" / "folder" / ".DS_Store")
      touch(template / "src" / "main" / "g8" / ".idea" / "ignoreMe")
      checkGeneratedProject(template, expected, actual)
  }

  it should "allow .g8ignore to take precedence over .gitignore" in testCase {
    case (template, expected, actual) =>
      "*"           >> (template / "src" / "main" / "g8" / ".gitignore")
      "*"           >> (expected / ".gitignore")
      "!.gitignore" >> (template / ".g8ignore")
      checkGeneratedProject(template, expected, actual)
  }

  it should "not ignore default files if .g8ignore is present" in testCase {
    case (template, expected, actual) =>

      val fileList = Seq(
        "giter8.sbt", "g8.sbt", "activator.properties", "template.properties", "test", "g8.test", "giter8.test"
      )

      touch(template / ".g8ignore")
      touch(template / "target" / "test")
      touch(template / ".git" / "foo")

      fileList.foreach {
        file =>
          touch(template / file)
          touch(template / "project" / file)
      }

      touch(expected / "target" / "test")
      touch(expected / ".git" / "foo")

      fileList.foreach {
        file =>
          touch(expected / file)
          touch(expected / "project" / file)
      }

      checkGeneratedProject(template, expected, actual)
  }

  private def testCase(test: (File, File, File) => Unit): Unit = {
    tempDirectory { tmp =>
      val templateDir = mkdir(tmp / "template")
      val outputDir   = mkdir(tmp / "output")
      val actualDir   = mkdir(tmp / "actual")
      test(templateDir, outputDir, actualDir)
    }
  }
}
