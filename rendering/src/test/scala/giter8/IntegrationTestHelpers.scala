package giter8

import java.io.{File, InputStream}

import org.scalatest.Matchers

import scala.io.Source
import scala.util.{Failure, Success, Try}

trait IntegrationTestHelpers extends FileContentMatchers { self: Matchers =>

  val mockHttpClient = new HttpClient {
    override def execute(request: HttpGetRequest): Try[HttpResponse] = Try {
      val responseBody =
        <metadata>
          <groupId>com.example</groupId>
          <artifactId>foo_2.12</artifactId>
          <versioning>
            <latest>1.2.3-SNAPSHOT</latest>
            <release>1.2.3-SNAPSHOT</release>
            <versions>
              <version>0.1.0</version>
              <version>1.0.0</version>
              <version>1.2.2-SNAPSHOT</version>
              <version>1.2.3-SNAPSHOT</version>
            </versions>
            <lastUpdated>20170217001002</lastUpdated>
          </versioning>
        </metadata>

      HttpResponse(200, "OK", Some(responseBody.mkString))
    }
  }

  def checkGeneratedProject(template: File,
                            expected: File,
                            actual: File,
                            httpClient: HttpClient = mockHttpClient): Unit = {
    Giter8Engine(httpClient).applyTemplate(template, None, actual, Map.empty, interactive = false) match {
      case Success(_)   => compareDirectories(actual, expected)
      case Failure(err) => fail(err)
    }
  }

  object InfiniteLineBreaks extends InputStream {
    override def read(): Int = '\n'.toByte
  }

  def compareDirectories(actual: File, expected: File): Unit = {
    compareDirectoryContents(actual, expected)
    compareFiles(actual, expected)
  }

  private def compareDirectoryContents(actual: File, expected: File): Unit = {
    val expectedFiles = getFiles(expected).keySet
    val actualFiles   = getFiles(actual).keySet

    val missingFiles = expectedFiles.diff(actualFiles)
    val missing      = if (missingFiles.nonEmpty) s"Missing files:\n\t${missingFiles.mkString("\n\t")}\n" else ""

    val extraFiles = actualFiles.diff(expectedFiles)
    val extra      = if (extraFiles.nonEmpty) s"Extra files:\n\t${extraFiles.mkString("\n\t")}\n" else ""

    val result = missing + extra
    if (result.nonEmpty) fail(s"$result")
  }

  private def compareFiles(actual: File, expected: File): Unit = {
    val expectedFiles = getFiles(expected)
    val actualFiles   = getFiles(actual)
    actualFiles foreach {
      case (path, file) =>
        compareFileContents(file, expectedFiles(path))
    }
  }

  private def compareFileContents(actual: File, expected: File): Unit = {
    val expectedLines = Source.fromFile(expected).getLines().mkString("\n")
    actual should haveContents(expectedLines)
  }

  private def getFiles(baseDir: File): Map[String, File] = {
    mapWithRelativePaths(getFilesRecursively(baseDir), baseDir)
  }

  private def mapWithRelativePaths(files: Seq[File], baseDir: File): Map[String, File] = {
    val pairs = files.map(f => getRelativePath(f, baseDir) -> f)
    Map(pairs: _*)
  }

  private def getRelativePath(file: File, baseDirectory: File): String = {
    file.getAbsolutePath.stripPrefix(baseDirectory.getAbsolutePath)
  }

  private def getFilesRecursively(file: File): Seq[File] = file match {
    case dir if file.isDirectory => dir.listFiles.flatMap(getFilesRecursively)
    case _                       => Seq(file)
  }
}
