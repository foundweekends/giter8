import sbt._

object Builds extends sbt.Build {
  import Keys._
  
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.2.3-SNAPSHOT",
    organization := "net.databinder",
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.9.1", "2.8.1"),
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Test, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    publishTo <<= version { (v: String) =>
      val nexus = "http://nexus.scala-tools.org/content/repositories/"
      if(v endsWith "-SNAPSHOT") Some("Scala Tools Nexus" at nexus + "snapshots/")
      else Some("Scala Tools Nexus" at nexus + "releases/")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),    
    publishMavenStyle := true
  )
  
  // posterous title needs to be giter8, so both app and root are named giter8
  lazy val root = Project("root", file("."),
    settings = buildSettings ++ Seq(
      name := "giter8" 
    )) aggregate(app)
  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ Seq(
      name := "giter8",
      libraryDependencies ++= { 
        Seq("org.scala-tools.sbt" % "launcher-interface" % "0.7.4" % "provided",
            "net.databinder" %% "dispatch-lift-json" % "0.8.5",
            "org.clapper" %% "scalasti" % "0.5.5")
      }
    ))
  lazy val plugin = Project("giter8-plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      libraryDependencies <++= (sbtDependency) { sd =>
        Seq(sd, "org.antlr" % "stringtemplate" % "3.2.1")
      }
    ))  
}
