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

import org.eclipse.jgit.transport.{CredentialItem, CredentialsProvider, URIish}

object ConsoleCredentialsProvider extends CredentialsProvider {

  def isInteractive = true

  def supports(items: CredentialItem*) = true

  def get(uri: URIish, items: CredentialItem*): Boolean = {
    if (System.console == null) false
    else {
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
  }

  @scala.annotation.tailrec
  def askYesNo(prompt: String): Boolean = {
    System.console.readLine("%s: ", prompt).trim.toLowerCase match {
      case "yes" => true
      case "no"  => false
      case _     => askYesNo(prompt)
    }
  }
}
