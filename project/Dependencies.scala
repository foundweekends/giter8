import sbt._

object Dependencies {
  val scalasti  = "org.clapper" %% "scalasti" % "2.1.2"
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.7.1" excludeAll (
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  // Picking jgit used by sbt-git
  // https://github.com/eclipse/jgit/blob/v3.7.0.201502260915-r/pom.xml
  // This uses httpclient 4.1
  // http://hc.apache.org/httpcomponents-client-4.2.x/
  val jgit             = "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "4.9.0.201710071750-r" excludeAll (
    ExclusionRule("javax.jms", "jms"),
    ExclusionRule("com.sun.jdmk", "jmxtools"),
    ExclusionRule("com.sun.jmx", "jmxri")
  )
  val scopt            = "com.github.scopt" %% "scopt" % "3.5.0"
  val scalacheck       = "org.scalacheck" %% "scalacheck" % "1.13.4"
  val scalatest        = "org.scalatest" %% "scalatest" % "3.0.1"
  val scalamock        = "org.scalamock" %% "scalamock-scalatest-support" % "3.4.2"
  val sbtIo            = "org.scala-sbt" %% "io" % "1.0.0-M7"
  val scala210         = "2.10.7"
  val scala211         = "2.11.12"
  val scala212         = "2.12.4"
  val scalaXml         = "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
  val parserCombinator = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5"
  val logback          = "ch.qos.logback" % "logback-classic" % "1.1.7"
}
