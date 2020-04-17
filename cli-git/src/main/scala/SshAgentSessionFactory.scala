package giter8

import java.nio.file.{Files, Path}

import com.jcraft.jsch.agentproxy.{ConnectorFactory, RemoteIdentityRepository}
import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.transport.{JschConfigSessionFactory, OpenSshConfig}
import org.eclipse.jgit.util.FS

class SshAgentSessionFactory(knownHosts: Option[String]) extends JschConfigSessionFactory() {

  private val Home = Option(System.getProperty("user.home")).getOrElse("")

  private val KnownHostsPaths: List[String] = List(
    s"$Home/.ssh/known_hosts",
    "/etc/ssh/ssh_known_hosts"
  )

  override protected def configure(host: OpenSshConfig.Host, session: Session): Unit = {}

  override protected def createDefaultJSch(fs: FS): JSch = {
    val jsch = new JSch()
    knownHostsWithDefault.foreach(jsch.setKnownHosts)
    jsch.setIdentityRepository(identityRepository)
    jsch
  }

  private def knownHostsWithDefault = {
    knownHosts.orElse(KnownHostsPaths.find(path => Files.exists(Path.of(path))))
  }

  private def identityRepository = {
    val factory   = ConnectorFactory.getDefault
    val connector = factory.createConnector
    new RemoteIdentityRepository(connector)
  }
}
