package giter8

import java.io.File

import org.apache.commons.io.FileUtils

import scala.util.Try

trait TempDir {

  protected def tempdir = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)

  /**
    * Runs a block in passing a `File` of a temporary dir to the function
    * cleans up the tempdir once the block runs
    *
    * @param block
    * @tparam A
    * @return
    */
  protected def withTempdir[A](block: File => A): Try[A] = {

    val file   = tempdir
    val result = Try(block(file))

    if (file.exists) FileUtils.forceDeleteOnExit(file)

    result
  }
}
