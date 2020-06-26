package testpkg

import giter8._
import verify._
import java.io.File
import sbt.io.IO

object LauncherTest extends BasicTestSuite {
  lazy val launcher = new Runner {
    def run(args: Array[String], workingDirectory: File): Int = {
      run(args, workingDirectory, new LauncherProcessor)
    }
  }
  implicit private class RichFile(file: File) {
    def /(child: String): File = new File(file, child)
  }

  test("runs scala/scala-seed.g8") {
    IO.withTemporaryDirectory { dir =>
      launcher.run(Array("scala/scala-seed.g8", "--name=hello"), dir)
      assert((dir / "hello" / "build.sbt").exists)
    }
  }

  /*
  test("runs git@github.com:scala/scala-seed.g8.git") {
    IO.withTemporaryDirectory { dir =>
      launcher.run(Array("git@github.com:scala/scala-seed.g8.git", "--name=hello"), dir)
      assert((dir / "hello" / "build.sbt").exists)
    }
  }
 */
}
