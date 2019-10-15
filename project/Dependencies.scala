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
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "5.5.1.201910021850-r" excludeAll (
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
  val scopt            = "com.github.scopt" %% "scopt" % "3.7.1"
  val scalacheck       = "org.scalacheck" %% "scalacheck" % "1.14.0"
  val scalatest        = "org.scalatest" %% "scalatest" % "3.0.8"
  val scalamock        = "org.scalamock" %% "scalamock" % "4.4.0"
  val sbtIo            = "org.scala-sbt" %% "io" % "1.3.0-M13"
  val scala212         = "2.12.10"
  val scala213         = "2.13.1"
  val sbt1             = "1.2.8"
  val scalaXml         = "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
  val parserCombinator = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
  val logback          = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val atto             = "org.tpolecat" %% "atto-core" % "0.7.0"
}
