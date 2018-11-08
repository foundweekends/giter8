import sbt._

object Dependencies {
  val stringTemplate = "org.antlr" % "ST4" % "4.0.8"
  val commonsIo      = "commons-io" % "commons-io" % "2.6"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.7.1" excludeAll (
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "5.1.3.201810200350-r" excludeAll (
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
  val scopt            = "com.github.scopt" %% "scopt" % "3.7.0"
  val scalacheck       = "org.scalacheck" %% "scalacheck" % "1.13.5"
  val scalatest        = "org.scalatest" %% "scalatest" % "3.0.5"
  val scalamock        = "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0"
  val sbtIo            = "org.scala-sbt" %% "io" % "1.2.2"
  val scala210         = "2.10.7"
  val scala211         = "2.11.12"
  val scala212         = "2.12.7"
  val sbt1             = "1.1.6"
  val sbt013           = "0.13.17"
  val scalaXml         = "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
  val parserCombinator = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0"
  val logback          = "ch.qos.logback" % "logback-classic" % "1.2.3"
}
