import sbt._

object Dependencies {
  val stringTemplate = "org.antlr" % "ST4" % "4.2"
  val commonsIo      = "commons-io" % "commons-io" % "2.6"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.7.1" excludeAll (
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "5.6.0.201912101111-r" excludeAll (
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
  val scopt            = "com.github.scopt" %% "scopt" % "3.7.1"
  val scalacheck       = "org.scalacheck" %% "scalacheck" % "1.14.3"
  val scalatest        = "org.scalatest" %% "scalatest" % "3.1.0"
  val scalamock        = "org.scalamock" %% "scalamock" % "4.4.0"
  val sbtIo            = "org.scala-sbt" %% "io" % "1.3.3"
  val scala212         = "2.12.10"
  val scala213         = "2.13.1"
  val sbt1             = "1.2.8"
  val scalaXml         = "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
  val parserCombinator = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
  val logback          = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val coursier         = "io.get-coursier" %% "coursier" % "2.0.0-RC6-5"
  val launcherIntf     = "org.scala-sbt" % "launcher-interface" % "1.1.3"
}
