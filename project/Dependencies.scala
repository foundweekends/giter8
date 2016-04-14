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
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r"
  val scopt = "com.github.scopt" %% "scopt" % "3.1.0"
  val dispatchJson = "net.databinder" %% "dispatch-json" % "0.8.10"
  val dispatchHttp = "net.databinder" %% "dispatch-http" % "0.8.10"
}
