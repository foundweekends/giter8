package giter8

import java.io.File

import giter8.GitInteractor.TransportError
import org.eclipse.jgit.api.errors.{RefAlreadyExistsException, TransportException}
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.api.{Git => JGit}

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

trait GitInteractor {
  def cloneRepository(url: String, dest: File): Try[Unit]
  def getRemoteBranches(url: String): Try[Seq[String]]
  def getDefaultBranch(repository: File): Try[String]
  def checkoutBranch(repository: File, branch: String): Try[Unit]
}

object GitInteractor {
  case class TransportError(message: String) extends RuntimeException(message)
}

class JGitInteractor extends GitInteractor {
  CredentialsProvider.setDefault(ConsoleCredentialsProvider)

  override def cloneRepository(url: String, dest: File): Try[Unit] = Try {
    JGit.cloneRepository().
      setURI(url).
      setDirectory(dest).
      setCredentialsProvider(ConsoleCredentialsProvider).
      call().
      close()
  }

  override def getRemoteBranches(url: String): Try[Seq[String]] = {
    Try(JGit.lsRemoteRepository().setRemote(url).setHeads(true).setTags(false).call()) map { refs =>
      refs.asScala.map(r => r.getName.stripPrefix("refs/heads/")).toSeq
    } recoverWith {
      case e: TransportException => Failure(TransportError(e.getMessage))
    }
  }

  override def getDefaultBranch(repository: File): Try[String] = Try {
    val git = JGit.open(repository)
    try {
      val refs = git.getRepository.getAllRefs.asScala
      // We assume we have freshly cloned repository with origin set up to clone URL
      // Symref HEAD will point to default remote branch.
      val symRefs = refs.filter(_._2.isSymbolic)
      val head = symRefs("HEAD")
      head.getTarget.getName.stripPrefix("refs/heads/")
    } finally git.close()
  }

  override def checkoutBranch(repository: File, branch: String): Try[Unit] = {
    val git = JGit.open(repository)
    if (git.getRepository.getBranch == branch) Success(())
    else {
    val checkoutCommand = git.checkout().setName(branch)
      Try {
        checkoutCommand.setCreateBranch(true).setStartPoint("origin/" + branch).call()
      } map { _ =>
        git.close()
      }
    }
  }
}
