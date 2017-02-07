package giter8

import scala.util.matching.Regex

sealed trait GitRepository

object GitRepository {
  case class Local(path: String) extends GitRepository

  case class Remote(url: String) extends GitRepository

  case class GitHub(user: String, repo: String) extends GitRepository {
    def publicUrl: String = s"git://github.com/$user/$repo.g8.git"
    def privateUrl: String = s"git@github.com:$user/$repo.g8.git"
  }

  def fromString(string: String): Either[String, GitRepository] = string match {
    case Matches.Local(path) => Right(Local(path))
    case Matches.NativeUrl(url) => Right(Remote(url))
    case Matches.HttpsUrl(url) => Right(Remote(url))
    case Matches.HttpUrl(url) => Right(Remote(url))
    case Matches.SshUrl(url) => Right(Remote(url))
    case Matches.GitHub(user, repo) => Right(GitHub(user, repo))
    case _ => Left(s"unknown repository type: $string")
  }

  object Matches {
    val GitHub: Regex = """^([^\s/]+)/([^\s/]+?)(?:\.g8)?$""".r
    val Local: Regex = """^file://(\S+)$""".r
    val NativeUrl: Regex = "^(git[@|://].*)$".r
    val HttpsUrl: Regex = "^(https://.*)$".r
    val HttpUrl: Regex = "^(http://.*)$".r
    val SshUrl: Regex = "^(ssh://.*)$".r
  }

}
