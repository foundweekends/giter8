package giter8

import org.eclipse.jgit.transport.{CredentialItem, CredentialsProvider, URIish}

object ConsoleCredentialsProvider extends CredentialsProvider {

  def isInteractive = true

  def supports(items: CredentialItem*) = true

  def get(uri: URIish, items: CredentialItem*): Boolean = {
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

      case i: CredentialItem.StringType if uri.getScheme == "ssh" =>
        val password = String.valueOf(System.console.readPassword("%s: ", i.getPromptText))
        i.setValue(password)
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
