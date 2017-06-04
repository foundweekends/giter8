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

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

sealed trait GitRepository

object GitRepository {
  case class UnknownRepositoryError(repository: String)
      extends RuntimeException(s"unknown repository type: $repository")

  case class Local(path: String) extends GitRepository
  case class Remote(url: String) extends GitRepository

  case class GitHub(user: String, repo: String) extends GitRepository {
    def publicUrl: String  = s"git://github.com/$user/$repo.g8.git"
    def privateUrl: String = s"git@github.com:$user/$repo.g8.git"
  }

  def fromString(repository: String): Try[GitRepository] = repository match {
    case Matches.Local(path)        => Success(Local(path))
    case Matches.NativeUrl(url)     => Success(Remote(url))
    case Matches.HttpsUrl(url)      => Success(Remote(url))
    case Matches.HttpUrl(url)       => Success(Remote(url))
    case Matches.SshUrl(url)        => Success(Remote(url))
    case Matches.GitHub(user, repo) => Success(GitHub(user, repo))
    case _                          => Failure(UnknownRepositoryError(repository))
  }

  object Matches {
    val GitHub: Regex    = """^([^\s/]+)/([^\s/]+?)(?:\.g8)?$""".r
    val Local: Regex     = """^file://(\S+)$""".r
    val NativeUrl: Regex = "^(git[@|://].*)$".r
    val HttpsUrl: Regex  = "^(https://.*)$".r
    val HttpUrl: Regex   = "^(http://.*)$".r
    val SshUrl: Regex    = "^(ssh://.*)$".r
  }

}
