import sbt._

object Dependencies {
  val stringTemplate = "org.antlr" % "ST4" % "4.3.4"
  val commonsIo      = "commons-io" % "commons-io" % "2.11.0"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "4.6.3" excludeAll (
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  // We excluded sshd-sftp to avoid https://github.com/advisories/GHSA-fhw8-8j55-vwgq
  // Either that or we need to bump to jgit 6.x
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.1.202206130422-r" excludeAll (
    ExclusionRule("org.apache.sshd", "sshd-sftp")
  )
  val jgitSshApache = "org.eclipse.jgit" % "org.eclipse.jgit.ssh.apache" % "5.13.1.202206130422-r" excludeAll (
    ExclusionRule("org.apache.sshd", "sshd-sftp")
  )
  val scopt      = "com.github.scopt" %% "scopt" % "4.1.0"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.17.0"
  val scalatest = Seq(
    "org.scalatest" %% "scalatest-flatspec" % "3.2.15" % Test,
    "org.scalatest" %% "scalatest-funspec" % "3.2.15" % Test,
    "org.scalatest" %% "scalatest-funsuite" % "3.2.15" % Test,
    "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.15" % Test
  )
  val scalamock = "org.scalamock" %% "scalamock" % "5.2.0"
  val verify    = "com.eed3si9n.verify" %% "verify" % "1.0.0"
  val sbtIo     = "org.scala-sbt" %% "io" % "1.8.0"
  val scala212  = "2.12.17"
  val scala213  = "2.13.10"
  val sbt1      = "1.2.8"
  val scalaXml  = "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
  def parserCombinator(scalaVersion: String) = "org.scala-lang.modules" %% "scala-parser-combinators" % {
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, _)) => "1.1.2" // Do not upgrade beyond 1.x
      case _            => "2.2.0"
    }
  }
  val logback      = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val coursier     = "io.get-coursier" %% "coursier" % "2.1.1"
  val launcherIntf = "org.scala-sbt" % "launcher-interface" % "1.4.1"
}
