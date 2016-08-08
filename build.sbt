import Dependencies._

val g8version = "0.6.9-SNAPSHOT"

// posterous title needs to be giter8, so both app and root are named giter8
lazy val root = (project in file(".")).
  aggregate(app, lib, scaffold, plugin).
  settings(
    inThisBuild(List(
      organization := "org.foundweekends.giter8",
      version := g8version,
      scalaVersion := "2.10.6",
      scalacOptions ++= Seq("-language:_", "-deprecation", "-Xlint"),
      publishArtifact in (Compile, packageBin) := true,
      homepage := Some(url("https://github.com/foundweekends/giter8")),
      publishMavenStyle := true,
      publishTo :=
        Some("releases" at
             "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
      publishArtifact in Test := false,
      licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      developers := List(
        Developer("n8han", "Nathan Hamblen", "@n8han", url("http://github.com/n8han"))
      ),
      scmInfo := Some(ScmInfo(url("https://github.com/foundweekends/giter8"), "git@github.com:foundweekends/giter8.git"))
    )),
    name := "giter8",
    publish := (),
    publishLocal := ()
  )

lazy val app = (project in file("app")).
  enablePlugins(ConscriptPlugin, BuildInfoPlugin).
  dependsOn(lib).
  settings(
    description := "Command line tool to apply templates defined on github",
    name := "giter8",
    sourceDirectory in csRun := { (baseDirectory).value.getParentFile / "src" / "main" / "conscript" },
    libraryDependencies ++= Seq(jgit, scopt, gigahorseCore),
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "giter8"
  )

lazy val scaffold = (project in file("scaffold")).
  dependsOn(lib).
  settings(
    name := "giter8-scaffold",
    description := "sbt plugin for scaffolding giter8 templates",
    sbtPlugin := true
  )

lazy val plugin = (project in file("plugin")).
  dependsOn(lib).
  settings(
    name := "giter8-plugin",
    scriptedSettings,
    description := "sbt plugin for testing giter8 templates",
    sbtPlugin := true,
    resolvers += Resolver.typesafeIvyRepo("releases"),
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted <<= ScriptedPlugin.scripted dependsOn(publishLocal in lib),
    libraryDependencies <+= sbtVersion("org.scala-sbt" % "scripted-plugin" % _)
  )

lazy val lib = (project in file("library")).
  settings(
    name := "giter8-lib",
    description := "shared library for app and plugin",
    libraryDependencies ++= Seq(
      scalasti, jline, gigahorseCore, commonsIo, plexusArchiver,
      scalacheck % Test, sbtIo % Test
    )
  )
