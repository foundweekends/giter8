import sbt._

object Dependencies {
  val stringTemplate = "org.antlr" % "ST4" % "4.3.1"
  val commonsIo      = "commons-io" % "commons-io" % "2.6"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.7.1" excludeAll (
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "5.9.0.202009080501-r" excludeAll (
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
  val jgitJsch             = "org.eclipse.jgit" % "org.eclipse.jgit.ssh.jsch" % "5.9.0.202009080501-r"
  val jsch                 = "com.jcraft" % "jsch.agentproxy.jsch" % "0.0.9"
  val jschSshAgent         = "com.jcraft" % "jsch.agentproxy.sshagent" % "0.0.9"
  val jschConnectorFactory = "com.jcraft" % "jsch.agentproxy.connector-factory" % "0.0.9"
  val scopt                = "com.github.scopt" %% "scopt" % "3.7.1"
  val scalacheck           = "org.scalacheck" %% "scalacheck" % "1.14.3"
  val scalatest            = "org.scalatest" %% "scalatest" % "3.2.2"
  val scalamock            = "org.scalamock" %% "scalamock" % "5.0.0"
  val verify               = "com.eed3si9n.verify" %% "verify" % "0.2.0"
  val sbtIo                = "org.scala-sbt" %% "io" % "1.3.4"
  val scala212             = "2.12.11"
  val scala213             = "2.13.2"
  val sbt1                 = "1.2.8"
  val scalaXml             = "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
  val parserCombinator     = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
  val logback              = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val coursier             = "io.get-coursier" %% "coursier" % "2.0.5"
  val launcherIntf         = "org.scala-sbt" % "launcher-interface" % "1.1.5"
}
