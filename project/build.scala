import sbt._

object Builds extends sbt.Build {
  import Keys._
  
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.3.2",
    organization := "net.databinder",
    scalaVersion := "2.9.1",
    libraryDependencies ++= Seq(
      "org.clapper" %% "scalasti" % "0.5.5"),
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Test, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    publishTo <<= version { (v: String) =>
      val nexus = "http://nexus.scala-tools.org/content/repositories/"
      if(v endsWith "-SNAPSHOT") Some("Scala Tools Nexus" at nexus + "snapshots/")
      else Some("Scala Tools Nexus" at nexus + "releases/")
    },
    resolvers += Resolver.url("databinder repo", url("http://databinder.net/repo"))(pattern),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),    
    publishMavenStyle := true
  )
  val pattern = Patterns(false, "[organisation]/[module]/[revision]/[type]s/[module](-[classifier]).[ext]")
  
  // posterous title needs to be giter8, so both app and root are named giter8
  lazy val root = Project("root", file("."),
    settings = buildSettings ++ Seq(
      name := "giter8" 
    )) aggregate(app, plugin, lsLibrary)
  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ conscript.Harness.conscriptSettings ++ Seq(
      name := "giter8",
      libraryDependencies +=
        "net.databinder" %% "dispatch-lift-json" % "0.8.5"
    )) dependsOn lsLibrary
  lazy val plugin = Project("giter8-plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      libraryDependencies <++= (sbtDependency, sbtVersion) { (sd, sv) =>
        Seq(sd,
            "org.scala-tools.sbt" %% "scripted-plugin" % sv
            )
      }
    ))
  lazy val lsLibrary =
    ProjectRef(uri("git://github.com/softprops/ls.git#29d66c5"), "library")
}
