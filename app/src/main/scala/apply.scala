package giter8

import java.io.File

import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.OpenSshConfig.Host
import org.eclipse.jgit.transport._
import org.eclipse.jgit.util.FS

import scala.util.{Failure, Try}

trait Apply { self: Giter8 =>
  import java.io.File

  import org.apache.commons.io.FileUtils
  import org.eclipse.jgit.api._

  private val tempdir =
    new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)


  /** Clean temporary directory used for git cloning */
  def cleanup() {
    if (tempdir.exists)
      FileUtils.forceDelete(tempdir)
  }

  val GitHub = """^([^\s/]+)/([^\s/]+?)(?:\.g8)?$""".r
  val Local = """^file://(\S+)$""".r

  object GitUrl {
    val NativeUrl = "^(git[@|://].*)$".r
    val HttpsUrl = "^(https://.*)$".r
    val HttpUrl = "^(http://.*)$".r

    def unapplySeq(s: Any): Option[List[String]] =
      NativeUrl.unapplySeq(s) orElse
      HttpsUrl.unapplySeq(s) orElse
      HttpUrl.unapplySeq(s)
  }

  def search(config: Config): Either[String, String] = {
    val prettyPrinter = (repo: GithubRepo) =>
      "%s \n\t %s\n" format (repo.name, repo.description)

    val repos = GithubRepo.search(config.repo)

    Right(repos.map(prettyPrinter).mkString(""))
  }

  def inspect(config: Config,
              arguments: Seq[String]): Either[String, String] = {
    config.repo match {
      case Local(path) =>
        val tmpl = config.branch.map { _ =>
          clone(path, config)
        }.getOrElse(copy(path))
        tmpl.right.flatMap { t =>
          G8Helpers.applyTemplate(t, new File("."), arguments, config.forceOverwrite)
        }
      case GitUrl(uri) =>
        val tmpl = clone(uri, config)
        tmpl.right.flatMap { t =>
          G8Helpers.applyTemplate(t,
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
          inspect(publicConfig, arguments)
        } catch {
          case _: org.eclipse.jgit.api.errors.JGitInternalException =>
            // assume it was an access failure, try with ssh
            // after cleaning the clone directory
            val privateConfig = config.copy(
              repo = "git@github.com:%s/%s.g8.git".format(user, proj)
            )
            cleanup()
            inspect(privateConfig, arguments)
        }
    }
  }

  def transportIdentity(identity: Option[String]=None) = {
    new IdentityTransportConfigCallback(new IdentitySessionFactory(identity))
  }

  def transportIdentity(password: String) = {
    new IdentityTransportConfigCallback(new IdentityPasswordSessionFactory(None, password))
  }

  def transportIdentity(identity: Option[String], password: String) = {
    new IdentityTransportConfigCallback(new IdentityPasswordSessionFactory(identity, password))
  }

  def clone(repo: String, config: Config) = {

    val branchName = config.branch.map("refs/heads/" + _)

    def doCall(cmd: CloneCommand): Git = {
      branchName.foreach(cmd.setBranch)
      cmd.call()
    }

    val g = Try {
      doCall(
        Git.cloneRepository()
          .setURI(repo)
          .setDirectory(tempdir)
          .setCredentialsProvider(ConsoleCredentialsProvider)
      )
    }.orElse( Try {
      cleanup()
      doCall(
        Git.cloneRepository()
          .setURI(repo)
          .setDirectory(tempdir)
          .setTransportConfigCallback(transportIdentity())
      )
    }).orElse( Try {
      cleanup()
      print("SSHCert Password: ")
      val password = String.valueOf(System.console().readPassword())
      doCall(
        Git.cloneRepository()
          .setURI(repo)
          .setDirectory(tempdir)
          .setTransportConfigCallback(transportIdentity(password))
      )
    }).get



    val result = branchName.map { b =>
      if(g.getRepository.getFullBranch.equals(b))
        Right(tempdir)
      else
        Left("Branch not found: " + b)
    } orElse config.tag.map{ t =>
      if (g.getRepository.getTags.containsKey(t)) {
        g.checkout().setName(t).call()
        Right(tempdir)
      } else {
        Left(s"Tag not found: refs/tags/$t")
      }
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
      Right(tempdir)
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

      case _ => // Ignore the rest
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

class IdentitySessionFactory(optIdentity: Option[String]=None) extends JschConfigSessionFactory {
  override def configure(hc: Host, session: Session): Unit = ()

  def getHomeIdentity(fs: FS): Option[String] = {
    Option(fs.userHome)
      .map(new File(_, ".ssh"))
      .filter(_.isDirectory)
      .map(new File(_, "id_rsa"))
      .map(_.getAbsolutePath)

  }

  def addIdentity(jsch: JSch, identity: String) =
    jsch.addIdentity(identity)

  override def createDefaultJSch(fs: FS): JSch = {
    val jsch = super.createDefaultJSch(fs)

    optIdentity.orElse(getHomeIdentity(fs)) match {
      case Some(identity) =>
        addIdentity(jsch, identity)
      case None =>
    }

    jsch
  }
}

class IdentityPasswordSessionFactory(identity: Option[String], password: String) extends IdentitySessionFactory {
  override def addIdentity(jsch: JSch, identity: String): Unit = {
    jsch.addIdentity(identity, password)
  }
}

class IdentityTransportConfigCallback(sessionFactory: JschConfigSessionFactory) extends TransportConfigCallback {
  override def configure(transport: Transport): Unit = {
    transport match {
      case transport:SshTransport =>
        transport.setSshSessionFactory(sessionFactory)
      case _ =>
    }
  }
}
