package giter8

import java.io.File

object Git {

  // The git clients, by priority
  val gitClients = GitCmd :: JGit :: Nil

  def clone(repo: String, config: Config, targetDir: File): Either[String, File] with Product with Serializable = {

    gitClients foreach { client =>
      try {
        val result = client.clone(repo, config, targetDir)
        return result // break if success
      } catch {
        case e: UnsupportedOperationException => // keep iterating
      }
    }
    Left("Could not find suitable git client.")
  }
}

trait Git {
  def clone(repo: String, config: Config, targetDir: File): Either[String, File] with Product with Serializable
}

class GitException(message: String, cause: Exception = null) extends Exception(message, cause)
