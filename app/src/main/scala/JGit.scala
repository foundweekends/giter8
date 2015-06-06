package giter8

import java.io.File

object JGit extends Git {

  def clone(repo: String, config: Config, targetDir: File): Either[String, File] with Product with Serializable = {
    import org.eclipse.jgit.api.errors.JGitInternalException
    import org.eclipse.jgit.api.{Git => EJGit}

    try {
      val cmd = EJGit.cloneRepository()
        .setURI(repo)
        .setDirectory(targetDir)
        .setCredentialsProvider(ConsoleCredentialsProvider)

      val branchName = config.branch.map("refs/heads/" + _)

      branchName.foreach(cmd.setBranch)

      val g = cmd.call()

      val result = branchName.map { b =>
        if (g.getRepository.getFullBranch.equals(b))
          Right(targetDir)
        else
          Left("Branch not found: " + b)
      } orElse config.tag.map { t =>
        if (g.getRepository.getTags.containsKey(t)) {
          g.checkout().setName(t).call()
          Right(targetDir)
        } else {
          Left(s"Tag not found: refs/tags/$t")
        }
      } getOrElse Right(targetDir)

      g.getRepository.close()
      result
    }
    catch {
      case e: JGitInternalException => throw new GitException(e.getMessage, e)
    }
  }
}

import org.eclipse.jgit.transport._

object ConsoleCredentialsProvider extends CredentialsProvider {

  def isInteractive = true

  def supports(items: CredentialItem*) = true

  def get(uri: URIish, items: CredentialItem*) = {
    items foreach {
      case i: CredentialItem.Username =>
        val username = System.console.readLine("%s: ", i.getPromptText)
        i.setValue(username)

      case i: CredentialItem.Password =>
        val password = System.console.readPassword("%s: ", i.getPromptText)
        i.setValueNoCopy(password)

      case i: CredentialItem.InformationalMessage =>
        System.console.printf("%s\n", i.getPromptText)

      case i: CredentialItem.YesNoType =>
        i.setValue(askYesNo(i.getPromptText))
    }
    true
  }

  @scala.annotation.tailrec
  def askYesNo(prompt: String): Boolean = {
    System.console.readLine("%s: ", prompt).trim.toLowerCase match {
      case "yes" => true
      case "no" => false
      case _ => askYesNo(prompt)
    }
  }
}

