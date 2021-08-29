package giter8

import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import java.{util => ju}
import java.io.File
import java.nio.file.{Path, Files, Paths}
import scala.collection.JavaConverters._

class SshAgentSessionFactory(knownHosts: Option[String]) extends SshdSessionFactory() {

  private val home        = Option(System.getProperty("user.home")).getOrElse("")
  private val programData = Option(System.getenv("PROGRAMDATA")).getOrElse("")

  private val defaultKnownHosts: List[String] = List(
    s"$home/.ssh/known_hosts",
    s"$home/.ssh/known_hosts2",
    "/etc/ssh/ssh_known_hosts",
    s"$programData/ssh/ssh_known_hosts"
  )

  override def getDefaultKnownHostsFiles(sshDir: File): ju.List[Path] = {
    knownHosts
      .map(_ :: Nil)
      .getOrElse(defaultKnownHosts)
      .map(Paths.get(_))
      .filter(Files.exists(_))
      .asJava
  }

}
