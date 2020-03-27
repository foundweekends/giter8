package giter8

import java.io.File

import org.scalatest.FunSuite
import G8._

class SampleTemplatesIntegrationTest extends FunSuite with IntegrationTestHelpers {
  import TestFileHelpers.tempDirectory

  case class TestCase(name: String, template: File, output: File)

  val testCases: Seq[TestCase] = {
    val testCasesDirectory = new File(getClass.getResource("/testcases").getPath)
    testCasesDirectory.listFiles map { testCase =>
      TestCase(testCase.getName, testCase / "template", testCase / "output")
    }
  }

  testCases foreach { testCase =>
    test(s"Test template: '${testCase.name}'") {
      tempDirectory { tmp => checkGeneratedProject(testCase.template, testCase.output, tmp) }
    }
  }
}
