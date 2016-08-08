import sbt._

object Dependencies {
  val scalasti = "org.clapper" %% "scalasti" % "2.0.0"
  val jline = ("jline" % "jline" % "1.0" force)
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val plexusArchiver = "org.codehaus.plexus" % "plexus-archiver" % "2.2" excludeAll(
    ExclusionRule("org.apache.commons", "commons-compress"),
    ExclusionRule("classworlds", "classworlds"),
    ExclusionRule("org.tukaani", "xz"),
    ExclusionRule("junit", "junit")
  )
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r"
  val scopt = "com.github.scopt" %% "scopt" % "3.1.0"
  val gigahorseCore = "com.eed3si9n" %% "gigahorse-core" % "0.1.1"
  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.0"
  val sbtIo = "org.scala-sbt" %% "io" % "1.0.0-M6"
}
