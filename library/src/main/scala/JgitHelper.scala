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

import java.io.{File, InputStream}

import org.apache.commons.io.filefilter.TrueFileFilter
import org.eclipse.jgit.ignore.FastIgnoreRule
import org.eclipse.jgit.transport._

import scala.collection.JavaConverters._
import scala.io.Source

class JGitIgnore(patterns: Seq[String]) {
  def getPatterns: Seq[String] = patterns

  def isIgnored(path: String): Boolean = {
    val ignoreRules = patterns.map(new FastIgnoreRule(_))
    ignoreRules.exists { rule =>
       rule.getResult && rule.isMatch(path, false)
    }
  }
}

object JGitIgnore {
  def apply(patterns: Seq[String]): JGitIgnore = new JGitIgnore(patterns)

  def apply(in: InputStream): JGitIgnore = {
    val patterns = Source.fromInputStream(in).getLines().toIndexedSeq
    new JGitIgnore(patterns)
  }

  def apply(file: File): JGitIgnore = {
    val patterns = Source.fromFile(file).
      getLines().
      filterNot(_.startsWith("#")).
      filterNot(_.trim.isEmpty).
      toIndexedSeq
    new JGitIgnore(patterns)
  }
}

object JgitHelper {
  import java.io.File
  import org.apache.commons.io.FileUtils
  import org.eclipse.jgit.api._

  private val tempdir =
    new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)

  /** Clean temporary directory used for git cloning */
  def cleanup(): Unit = {
    if (tempdir.exists)
      FileUtils.forceDelete(tempdir)
  }

  val GitHub = """^([^\s/]+)/([^\s/]+?)(?:\.g8)?$""".r
  val Local = """^file://(\S+)$""".r

  object GitUrl {
    val NativeUrl = "^(git[@|://].*)$".r
    val HttpsUrl = "^(https://.*)$".r
    val HttpUrl = "^(http://.*)$".r
    val SshUrl = "^(ssh://.*)$".r

    def unapplySeq(s: CharSequence): Option[List[String]] =
      NativeUrl.unapplySeq(s) orElse
      HttpsUrl.unapplySeq(s) orElse
      HttpUrl.unapplySeq(s) orElse
      SshUrl.unapplySeq(s)
  }

  def run(config: Config,
              arguments: Seq[String]): Either[String, String] = {
    config.repo match {
      case Local(path) =>
        val tmpl = config.branch.map { _ =>
          clone(path, config)
        }.getOrElse(copy(path))
        tmpl.right.flatMap { t =>
          G8.fromDirectory(t, new File("."), arguments, config.forceOverwrite)
        }
      case GitUrl(uri) =>
        val tmpl = clone(uri, config)
        tmpl.right.flatMap { t =>
          G8.fromDirectory(t,
            new File("."),
            arguments,
            config.forceOverwrite
          )
        }
      case GitHub(user, proj) =>
        try {
          val publicConfig = config.copy(
            repo = "git://github.com/%s/%s.g8.git".format(user, proj)
          )
          run(publicConfig, arguments)
        } catch {
          case _: org.eclipse.jgit.api.errors.TransportException =>
            // assume it was an access failure, try with ssh
            // after cleaning the clone directory
            val privateConfig = config.copy(
              repo = "git@github.com:%s/%s.g8.git".format(user, proj)
            )
            cleanup()
            run(privateConfig, arguments)
          // The following code can fallback to non-g8 repo, but because
          // of the stringtemplate's $, it's not like you can just point at any repo.
          // case _: org.eclipse.jgit.api.errors.TransportException =>
          //   val nonG8Config = config.copy(
          //     repo = "git://github.com/%s/%s.git".format(user, proj)
          //   )
          //   cleanup()
          //   run(nonG8Config, arguments)
        }
    }
  }

  def clone(repo: String, config: Config) = {
    import scala.collection.JavaConverters._

    val cmd = Git.cloneRepository()
      .setURI(repo)
      .setDirectory(tempdir)
      .setCredentialsProvider(ConsoleCredentialsProvider)

    val branchName = config.branch.map("refs/heads/" + _)
    for(b <- branchName)
      cmd.setBranch(b)

    val g = cmd.call()

    val result = branchName.map { b =>
      if(g.branchList().call().asScala.map(_.getName).contains(b))
        Right(tempdir)
      else
        Left("Branch not found: " + b)
    } getOrElse(Right(tempdir))
    g.getRepository.close()
    result
  }

  /** for file:// repositories with no named branch, just do a file copy */
  def copy(filename: String) = {
    val dir = new File(filename)
    if (!dir.isDirectory)
      Left("Not a readable directory: " + filename)
    else {
      FileUtils.copyDirectory(dir, tempdir)
      copyExecutableAttribute(dir, tempdir)
      Right(tempdir)
    }
  }

  private[this] def copyExecutableAttribute(fromDir: File, toDir: File) = {
    FileUtils.iterateFiles(fromDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).asScala.filter(_.canExecute).foreach { file =>
      val realtivePath = fromDir.toURI.relativize(file.toURI).getPath
      new File(toDir, realtivePath).setExecutable(true)
    }
  }

}

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
