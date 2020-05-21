package giter8

import java.io.File

import G8._
import org.scalatest.funsuite.AnyFunSuite
import scala.util.Properties

class SampleTemplatesIntegrationTest extends AnyFunSuite with IntegrationTestHelpers {
  import TestFileHelpers.tempDirectory

  case class TestCase(name: String, template: File, output: File)

  val testCases: Seq[TestCase] = {
    val testCasesDirectory = new File(getClass.getResource("/testcases").getPath)
    if (Properties.isWin) {
      // TODO fix and enable tests
      Nil
    } else {
      testCasesDirectory.listFiles map { testCase =>
        TestCase(testCase.getName, testCase / "template", testCase / "output")
      }
    }
  }

  testCases foreach { testCase =>
    test(s"Test template: '${testCase.name}'") {
      tempDirectory { tmp => checkGeneratedProject(testCase.template, testCase.output, tmp) }
    }
  }
}
