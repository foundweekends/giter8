package giter8

/*
 * Original implementation (C) 2010-2015 Nathan Hamblen and contributors
 * Adapted and extended in 2016 by foundweekends project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.{File, PrintWriter}
import java.nio.file.Files

import org.apache.commons.io.FileUtils

trait TestFileHelpers {
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

  def symLink(link: File, target: File): Unit = if(!link.exists()) {
    Files.createSymbolicLink(link.toPath, target.toPath)
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
