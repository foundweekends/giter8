package giter8

import java.io.{File, PrintWriter}

import org.apache.commons.io.FileUtils

object TestFileHelpers {
  def tempDirectory(code: File => Unit): Unit = {
    val tempDir = new File(FileUtils.getTempDirectory, "g8test-" + System.nanoTime)
    tempDir.mkdirs()
    code(tempDir)
    tempDir.delete()
  }

  def mkdir(dir: File): File = {
    if (dir.exists()) throw new Exception(s"${dir.getAbsolutePath} already exists")
    if (!dir.mkdirs()) throw new Exception(s"Cannot create ${dir.getAbsolutePath}")
    else dir
  }

  def touch(file: File): Unit = if (!file.exists) {
    file.getParentFile.mkdirs()
    file.createNewFile()
  }

  implicit class WriteableString(s: String) {
    def >>(file: File): Unit = {
      touch(file)
      new PrintWriter(file) {
        write(s)
        close()
      }
    }
  }
}
