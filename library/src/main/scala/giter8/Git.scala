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
import giter8.GitRepository.{GitHub, Local, Remote}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter

import scala.language.implicitConversions
import scala.util.{Failure, Try}
import scala.collection.JavaConverters._

class Git(gitInteractor: GitInteractor) {
  import Git._

  def clone(repository: GitRepository, ref: Option[Ref], destination: File): Try[Unit] = repository match {
    case remote: Remote => cloneWithGivenRefOrDefaultBranch(remote.url, ref, destination)
    case local: Local => ref match {
      // for file:// repositories with no named branch, just do a file copy (assume current branch)
      case None => copy(new File(local.path), destination)
      case Some(_) => cloneWithGivenRefOrDefaultBranch(local.path, ref, destination)
    }
    case github: GitHub => cloneWithGivenRefOrDefaultBranch(github.publicUrl, ref, destination) recoverWith {
      case _: TransportError =>
        cleanDir(destination)
        cloneWithGivenRefOrDefaultBranch(github.privateUrl, ref, destination)
    }
  }

  private def cloneWithGivenRefOrDefaultBranch(url: String, ref: Option[Ref], dest: File): Try[Unit] = ref match {
    case None => gitInteractor.cloneRepository(url, dest) flatMap { _ =>
      gitInteractor.getDefaultBranch(dest) flatMap { branch =>
        gitInteractor.checkoutBranch(dest, branch)
      }
    }
    case Some(Branch(br)) => gitInteractor.getRemoteBranches(url) flatMap { remoteBranches =>
      if (!remoteBranches.contains(br)) Failure(NoBranchError(br))
      else gitInteractor.cloneRepository(url, dest) flatMap { _ =>
        gitInteractor.checkoutBranch(dest, br)
      }
    }
    case Some(Tag(t)) => gitInteractor.getRemoteTags(url) flatMap { remoteTags =>
      if (!remoteTags.contains(t)) Failure(NoTagError(t))
      else gitInteractor.cloneRepository(url, dest) flatMap { _ =>
        gitInteractor.checkoutTag(dest, t)
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
  case class NoTagError(tagName: String) extends RuntimeException(s"No tag $tagName")
}

