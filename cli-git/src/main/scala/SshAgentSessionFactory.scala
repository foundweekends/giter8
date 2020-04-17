package giter8

import com.jcraft.jsch.agentproxy.{ConnectorFactory, RemoteIdentityRepository}
import com.jcraft.jsch.{JSch, Session}
import org.eclipse.jgit.transport.{JschConfigSessionFactory, OpenSshConfig}
import org.eclipse.jgit.util.FS

object SshAgentSessionFactory extends JschConfigSessionFactory() {

  override protected def configure(host: OpenSshConfig.Host, session: Session): Unit = {}

  override protected def createDefaultJSch(fs: FS): JSch = {
    val jsch = new JSch()
    JSch.setConfig("StrictHostKeyChecking", "no");
    jsch.setIdentityRepository(identityRepository)
    jsch
  }

  private def identityRepository = {
    val factory   = ConnectorFactory.getDefault
    val connector = factory.createConnector
    new RemoteIdentityRepository(connector)
  }
}
