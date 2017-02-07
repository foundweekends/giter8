package giter8

import java.io.File

import giter8.GitInteractor.TransportError
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.api.{Git => JGit}

import scala.util.{Failure, Try}
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

  override def getDefaultBranch(repository: File): Try[String] = withRepository(repository) { git =>
    val refs = git.getRepository.getAllRefs.asScala
    // We assume we have freshly cloned repository with origin set up to clone URL
    // Symref HEAD will point to default remote branch.
    val symRefs = refs.filter(_._2.isSymbolic)
    val head = symRefs("HEAD")
    head.getTarget.getName.stripPrefix("refs/heads/")
  }

  override def checkoutBranch(repository: File, branch: String): Try[Unit] = withRepository(repository) { git =>
    git.checkout().
      setCreateBranch(true).
      setName(branch).
      setStartPoint("origin/" + branch).
      call()
  }

  private def withRepository[A](repository: File)(code: JGit => A): Try[A] = Try {
    val git = JGit.open(repository)
    val result = code(git)
    git.close()
    result
  }
}
