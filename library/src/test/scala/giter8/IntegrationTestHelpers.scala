package giter8

import java.io.{File, InputStream}

import org.scalatest.Assertions._

import scala.io.Source

trait IntegrationTestHelpers {
  def checkGeneratedProject(template: File, expected: File, actual: File): Unit = {
    Console.withIn(InfiniteLineBreaks) {
      G8.fromDirectory(template, Left(actual), Seq.empty, forceOverwrite = false) match {
        case Right(_)  => compareDirectories(actual, expected)
        case Left(err) => fail(err)
      }
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
        compareFileContents(path, file, expectedFiles(path))
    }
  }

  private def compareFileContents(path: String, actual: File, expected: File): Unit = {
    val actualLines   = Source.fromFile(actual).getLines().toSeq
    val expectedLines = Source.fromFile(expected).getLines().toSeq
    expectedLines.zipWithIndex foreach {
      case (line, i) =>
        assert(line == actualLines(i), s"in file $path:$i")
    }
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
