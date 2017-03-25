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

import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success}

sealed trait Ref

case class Tag(name: String) extends Ref

case class Branch(name: String) extends Ref

case class Config(repo: String, ref: Option[Ref] = None, forceOverwrite: Boolean = false, directory: Option[String] = None)

class JgitHelper(gitInteractor: Git, templateRenderer: TemplateRenderer) {

  // Workaround before moving to Scala 2.12
  implicit class flatMapMonad[L, R](either: Either[L, R]) {
    def flatMap[R1](f: R => Either[L, R1]): Either[L, R1] = either match {
      case Right(r) => f(r)
      case Left(l) => Left(l)
    }

    def map[R1](f: R => R1): Either[L, R1] = either match {
      case Right(r) => Right(f(r))
      case Left(l) => Left(l)
    }
  }

  private val tempdir = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)

  /** Clean temporary directory used for git cloning */
  def cleanup(): Unit = if (tempdir.exists) FileUtils.forceDelete(tempdir)

  def run(config: Config, arguments: Seq[String], outDirectory: File): Either[String, String] = for {
    repository <- GitRepository.fromString(config.repo)
    baseDir <- gitInteractor.clone(repository, config.ref, tempdir) match {
      case Success(_) => Right(new File(tempdir, config.directory.getOrElse("")))
      case Failure(e) => Left(e.getMessage)
    }
    renderedTemplate <- templateRenderer.render(baseDir, outDirectory, arguments, config.forceOverwrite)
  } yield renderedTemplate

}
