package giter8

import java.io.File

import org.scalatest.{FunSuite, Matchers}

class SampleTemplatesIntegrationTest extends FunSuite with IntegrationTestHelpers with Matchers with TestFileHelpers {
  import FileDsl._

  case class TestCase(name: String, template: File, output: File)

  def testCaseDirectories(): Seq[File] = {
    val resourcesDirectory = Option(new File(getClass.getResource("/").getPath))
    resourcesDirectory match {
      case None => Seq.empty
      case Some(directory) =>
        directory.listFiles.filter { f =>
          f.isDirectory && f.getName != "giter8" && f.list().contains("template") && f.list().contains("output")
        }
    }
  }

  val testCases: Seq[TestCase] = testCaseDirectories map { dir =>
    TestCase(dir.getName, dir / "template", dir / "output")
  }

  testCases foreach { testCase =>
    test(s"Test template: '${testCase.name}'") {
      tempDirectory { tmp =>
        checkGeneratedProject(testCase.template, testCase.output, tmp)
      }
    }
  }
}
