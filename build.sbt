import Dependencies._
import sbt.Def

lazy val g8version = "0.8.1-SNAPSHOT"

lazy val commonScriptedSettings = scriptedSettings ++ Seq(
  scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
    a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
  ),
  scriptedBufferLog := false,
  scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
  scripted := ScriptedPlugin.scripted.dependsOn(publishLocal in rendering).evaluated,
  test in Test := scripted.toTask("").value
)

lazy val scala211libraryDependencies = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq(scalaXml, parserCombinator)
    case _                                         => Nil
  }
}

// posterous title needs to be giter8, so both app and root are named giter8
lazy val root = (project in file("."))
  .enablePlugins(NoPublish)
  .enablePlugins(TravisSitePlugin)
  .aggregate(app, rendering, resolving, scaffold, plugin)
  .settings(
    inThisBuild(
      Seq(
        organization := "org.foundweekends.giter8",
        version := g8version,
        scalaVersion := "2.10.6",
        organizationName := "foundweekends",
        organizationHomepage := Some(url("http://foundweekends.org/")),
        scalacOptions ++= Seq("-language:_", "-deprecation", "-Xlint", "-Xfuture"),
        publishArtifact in (Compile, packageBin) := true,
        homepage := Some(url("http://www.foundweekends.org/giter8/")),
        publishMavenStyle := true,
        publishTo :=
          Some("releases" at
            "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
        publishArtifact in Test := false,
        parallelExecution in Test := false,
        licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
        developers := List(
          Developer("n8han", "Nathan Hamblen", "@n8han", url("http://github.com/n8han")),
          Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
        ),
        scmInfo := Some(
          ScmInfo(url("https://github.com/foundweekends/giter8"), "git@github.com:foundweekends/giter8.git"))
      )),
    siteGithubRepo := "foundweekends/giter8",
    siteEmail := { "eed3si9n" + "@" + "gmail.com" },
    customCommands
  )

lazy val app = (project in file("app"))
  .disablePlugins(BintrayPlugin)
  .enablePlugins(ConscriptPlugin)
  .enablePlugins(SonatypePublish)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(resolving, rendering)
  .settings(
    name := "giter8",
    description := "Command line tool to apply templates defined on github",
    crossScalaVersions := List(scala210, scala211, scala212),
    sourceDirectory in csRun := baseDirectory.value.getParentFile / "src" / "main" / "conscript",
    libraryDependencies ++= Seq(
      scopt,
      logback,
      jgit,
      scalatest % Test,
      scalamock % Test,
      slf4jSimple % Test
    ),
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "giter8"
  )

lazy val scaffold = (project in file("scaffold"))
  .enablePlugins(BintrayPublish)
  .dependsOn(rendering)
  .settings(commonScriptedSettings)
  .settings(
    name := "sbt-giter8-scaffold",
    description := "sbt plugin for scaffolding giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala210)
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(BintrayPublish)
  .dependsOn(rendering)
  .settings(commonScriptedSettings)
  .settings(
    name := "sbt-giter8",
    description := "sbt plugin for testing giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala210),
    resolvers += Resolver.typesafeIvyRepo("releases"),
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
    )
  )

lazy val rendering = (project in file("rendering"))
  .disablePlugins(BintrayPlugin)
  .enablePlugins(SonatypePublish)
  .settings(
    name := "giter8-rendering",
    description := "Functionality for rendering templates, used within Giter8 app and plugin",
    excludeFilter in (Test, unmanagedResources) := ".DS_Store",
    unmanagedResourceDirectories in Test += file("examples"),
    crossScalaVersions := List(scala210, scala211, scala212),
    libraryDependencies ++= scala211libraryDependencies.value,
    libraryDependencies ++= Seq(
      jsr305 % Compile,
      jgit,
      scalasti,
      commonsIo,
      plexusArchiver,
      scalacheck % Test,
      scalatest % Test,
      scalamock % Test,
      slf4jSimple % Test
    )
  )

lazy val resolving = (project in file("resolving"))
  .disablePlugins(BintrayPlugin)
  .enablePlugins(SonatypePublish)
  .settings(
    name := "giter8-resolving",
    description := "Functionality to resolve templates, used within Giter8 app and plugin",
    excludeFilter in (Test, unmanagedResources) := ".DS_Store",
    unmanagedResourceDirectories in Test += file("examples"),
    crossScalaVersions := List(scala210, scala211, scala212),
    libraryDependencies ++= scala211libraryDependencies.value,
    libraryDependencies ++= Seq(
      jsr305 % Compile,
      jgit,
      commonsIo,
      plexusArchiver,
      scalacheck % Test,
      scalatest % Test,
      scalamock % Test,
      slf4jSimple % Test
    )
  )

def customCommands: Seq[Setting[_]] = Seq(
  commands += Command.command("release") { state =>
    "clean" ::
      "so compile" ::
      "so publishSigned" ::
      "reload" ::
      state
  }
)
