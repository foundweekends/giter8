package giter8

import java.io.File

import giter8.GitInteractor.TransportError
import giter8.GitRepository.{GitHub, Local, Remote}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter

import scala.language.implicitConversions
import scala.util.{Failure, Try}
import scala.collection.JavaConverters._

class Git(gitInteractor: GitInteractor) {
  import Git._

  def clone(repository: GitRepository, branch: Option[String], destination: File): Try[Unit] = repository match {
    case remote: Remote => cloneWithGivenOrDefaultBranch(remote.url, branch, destination)
    case local: Local => branch match {
      // for file:// repositories with no named branch, just do a file copy (assume current branch)
      case None => copy(new File(local.path), destination)
      case Some(_) => cloneWithGivenOrDefaultBranch(local.path, branch, destination)
    }
    case github: GitHub => cloneWithGivenOrDefaultBranch(github.publicUrl, branch, destination) recoverWith {
      case _: TransportError =>
        cleanDir(destination)
        cloneWithGivenOrDefaultBranch(github.privateUrl, branch, destination)
    }
  }

  private def cloneWithGivenOrDefaultBranch(url: String, branch: Option[String], dest: File): Try[Unit] = branch match {
    case None => gitInteractor.cloneRepository(url, dest) flatMap { _ =>
      gitInteractor.getDefaultBranch(dest) flatMap { branch =>
        gitInteractor.checkoutBranch(dest, branch)
      }
    }
    case Some(br) => gitInteractor.getRemoteBranches(url) flatMap { remoteBranches =>
      if (!remoteBranches.contains(br)) Failure(NoBranchError(br))
      else gitInteractor.cloneRepository(url, dest) flatMap { _ =>
        gitInteractor.checkoutBranch(dest, br)
      }
    }
  }

  // Protected for testing: see GitTest.scala
  protected def cleanDir(dir: File): Unit = dir.listFiles().foreach(_.delete())

  // Protected for testing: see GitTest.scala
  protected def copy(from: File, to: File): Try[Unit] = Try {
    if (!from.isDirectory) throw CloneError("Not a readable directory: " + from.getAbsolutePath)
    FileUtils.copyDirectory(from, to)
    copyExecutableAttribute(from, to)
  }

  private def copyExecutableAttribute(fromDir: File, toDir: File): Unit = {
    val files = FileUtils.iterateFiles(fromDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).asScala
    val executables = files.filter(_.canExecute)
    executables foreach {
      file =>
        val relativePath = fromDir.toURI.relativize(file.toURI).getPath
        new File(toDir, relativePath).setExecutable(true)
    }
  }
}

object Git {
  case class CloneError(message: String) extends RuntimeException(message)
  case class NoBranchError(branchName: String) extends RuntimeException(s"No branch $branchName")
}
