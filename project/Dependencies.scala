import sbt._

object Dependencies {
  val scalasti = "org.clapper" %% "scalasti" % "2.0.0"
  val jline = ("jline" % "jline" % "1.0" force)
  val lsCore = "me.lessis" %% "ls" % "0.1.3"
  // override ls's older version of dispatch
  val dispatchCore = "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.2" excludeAll(
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "4.4.1.201607150455-r"
  val scopt = "com.github.scopt" %% "scopt" % "3.5.0"
  val httpClient = "org.apache.httpcomponents" % "httpclient" % "4.3.6"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.0"
  val sbtIo = "org.scala-sbt" %% "io" % "1.0.0-M6"
}
