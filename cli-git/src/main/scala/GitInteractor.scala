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

package giter8

import java.io.File

import giter8.GitInteractor.TransportError
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.api.{Git => JGit}

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

trait GitInteractor {
  def cloneRepository(url: String, dest: File): Try[Unit]
  def getRemoteBranches(url: String): Try[Seq[String]]
  def getRemoteTags(url: String): Try[Seq[String]]
  def getDefaultBranch(repository: File): Try[String]
  def checkoutBranch(repository: File, branch: String): Try[Unit]
  def checkoutTag(repository: File, tag: String): Try[Unit]
}

object GitInteractor {
  case class TransportError(message: String) extends RuntimeException(message)
}

class JGitInteractor extends GitInteractor {
  CredentialsProvider.setDefault(ConsoleCredentialsProvider)

  override def cloneRepository(url: String, dest: File): Try[Unit] = Try {
    JGit
      .cloneRepository()
      .setURI(url)
      .setDirectory(dest)
      .setCredentialsProvider(ConsoleCredentialsProvider)
      .call()
      .close()
  }

  override def getRemoteBranches(url: String): Try[Seq[String]] = {
    Try(JGit.lsRemoteRepository().setRemote(url).setHeads(true).setTags(false).call()) map { refs =>
      refs.asScala.map(r => r.getName.stripPrefix("refs/heads/")).toSeq
    } recoverWith {
      case e: TransportException => Failure(TransportError(e.getMessage))
    }
  }

  override def getRemoteTags(url: String): Try[Seq[String]] = {
    Try(JGit.lsRemoteRepository().setRemote(url).setHeads(false).setTags(true).call()) map { refs =>
      refs.asScala.map(r => r.getName.stripPrefix("refs/tags/")).toSeq
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
      val head    = symRefs("HEAD")
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
      } map { _ => git.close() }
    }
  }

  override def checkoutTag(repository: File, tag: String): Try[Unit] = Try {
    val git = JGit.open(repository)
    Try(git.checkout().setName(tag).call()).map(_ => git.close())
  }
}
