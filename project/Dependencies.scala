import sbt._

object Dependencies {
  val stringTemplate = "org.antlr" % "ST4" % "4.3.2"
  val commonsIo      = "commons-io" % "commons-io" % "2.11.0"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.7.1" excludeAll (
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "5.13.0.202109080827-r" excludeAll (
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
  val jgitJsch             = "org.eclipse.jgit" % "org.eclipse.jgit.ssh.jsch" % "5.13.0.202109080827-r"
  val jsch                 = "com.jcraft" % "jsch.agentproxy.jsch" % "0.0.9"
  val jschSshAgent         = "com.jcraft" % "jsch.agentproxy.sshagent" % "0.0.9"
  val jschConnectorFactory = "com.jcraft" % "jsch.agentproxy.connector-factory" % "0.0.9"
  val scopt                = "com.github.scopt" %% "scopt" % "4.0.1"
  val scalacheck           = "org.scalacheck" %% "scalacheck" % "1.15.4"
  val scalatest            = "org.scalatest" %% "scalatest" % "3.2.11"
  val scalamock            = "org.scalamock" %% "scalamock" % "5.2.0"
  val verify               = "com.eed3si9n.verify" %% "verify" % "1.0.0"
  val sbtIo                = "org.scala-sbt" %% "io" % "1.6.0"
  val scala212             = "2.12.15"
  val scala213             = "2.13.8"
  val sbt1                 = "1.2.8"
  val scalaXml             = "org.scala-lang.modules" %% "scala-xml" % "2.0.1"
  val parserCombinator     = "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
  val logback              = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val coursier             = "io.get-coursier" %% "coursier" % "2.0.16"
  val launcherIntf         = "org.scala-sbt" % "launcher-interface" % "1.3.3"
}
