package giter8

import java.io.{IOException, File}
import scala.util.{Failure, Success, Try}
import sys.process._

object GitCmd extends Git {
  def clone(url: String, config: Config, targetDir: File): Either[String, File] with Product with Serializable = {

    // $ git clone --single-branch --depth 1 --branch $branchName $url
    val gitCmdHead = "git clone --single-branch --depth 1"
    import config._
    val branchCmd =
      if (tag.isDefined) s"--branch refs/tags/${tag.get}"
      else if (branch.isDefined) s"--branch ${branch.get}"
      else ""
    val gitCmd = s"$gitCmdHead $branchCmd $url ${targetDir.getAbsolutePath}"
    val logger = new ErrorLogger
    val result = Try { gitCmd ! logger }
    result match {
      case Success(127) => throw new UnsupportedOperationException
      case Success(0) => Right(targetDir)
      case Failure(e: IOException) => throw new UnsupportedOperationException(e.getMessage, e)
      case Failure(e) => throw e
      case _ =>
        val errorMsg = logger.errorMsg.toString
        import config._
        if (tag.isDefined && errorMsg.contains(s"branch refs/tags/${tag.get} not found"))
          Left(s"Tag not found: refs/tags/${tag.get}")
        else if (branch.isDefined && errorMsg.contains(s"branch ${branch.get} not found"))
          Left(s"Branch not found: ${branch.get}")
        else
          throw new GitException(s"$gitCmd\n$errorMsg")
    }
  }

  class ErrorLogger extends ProcessLogger {

    // We're not sure about thread safety and this is not performance-critical. Use StringBuffer, not StringBuilder.
    val errorMsg = new StringBuffer(1024)

    override def buffer[T](f: => T): T = f

    override def out(s: => String): Unit = {
      // Do nothing. We don't capture stdout
    }

    override def err(s: => String): Unit = if (errorMsg.length > 0) errorMsg.append('\n' + s) else errorMsg.append(s)
  }
}
