package testpkg

import giter8._
import verify._
import java.io.{ByteArrayOutputStream, File, PrintStream}
import sbt.io.IO

object LauncherTest extends BasicTestSuite {
  object launcher extends Runner {
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

  test("log scala/scala-seed.g8") {
    val (r, err) = withErr {
      IO.withTemporaryDirectory { dir =>
        launcher.run(Array("scala/scala-seed.g8", "--name=hello"), dir)
        assert((dir / "hello" / "build.sbt").exists)
      }
    }
    assert(!err.contains("SLF4J"))
  }

  def withErr[A1](f: => A1): (A1, String) = {
    val originalErr           = System.err
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val inMemoryPrintStream   = new PrintStream(byteArrayOutputStream)
    val result                =
      try {
        System.setErr(inMemoryPrintStream)
        val r = f
        inMemoryPrintStream.flush()
        r
      } finally {
        System.setErr(originalErr)
      }
    (result, byteArrayOutputStream.toString)
  }

}
