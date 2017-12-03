import Dependencies._
import CrossVersion.partialVersion

val g8version = "0.10.1-SNAPSHOT"

// posterous title needs to be giter8, so both app and root are named giter8
lazy val root = (project in file("."))
  .enablePlugins(TravisSitePlugin, NoPublish)
  .aggregate(app, lib, scaffold, plugin)
  .settings(
    inThisBuild(
      List(
        organization := "org.foundweekends.giter8",
        version := g8version,
        scalaVersion := scala210,
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
    name := "giter8",
    crossScalaVersions := List(scala210, scala211, scala212),
    siteGithubRepo := "foundweekends/giter8",
    siteEmail := { "eed3si9n" + "@" + "gmail.com" },
    customCommands
  )

lazy val app = (project in file("app"))
  .disablePlugins(BintrayPlugin)
  .enablePlugins(ConscriptPlugin, BuildInfoPlugin, SonatypePublish)
  .dependsOn(lib)
  .settings(
    description := "Command line tool to apply templates defined on github",
    name := "giter8",
    crossScalaVersions := List(scala210, scala211, scala212),
    sourceDirectory in csRun := {
      (baseDirectory).value.getParentFile / "src" / "main" / "conscript"
    },
    libraryDependencies ++= Seq(scopt, logback),
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "giter8"
  )

lazy val crossSbt = Seq(
  crossSbtVersions := List("0.13.16", "1.0.4"),
  scalaVersion := {
    val crossSbtVersion = (sbtVersion in pluginCrossBuild).value
    partialVersion(crossSbtVersion) match {
      case Some((0, 13)) => scala210
      case Some((1, _)) => scala212
      case _ =>
        throw new Exception(s"unexpected sbt version: $crossSbtVersion (supported: 0.13.x or 1.X)")
    }
  }
)

lazy val scaffold = (project in file("scaffold"))
  .enablePlugins(BintrayPublish)
  .dependsOn(lib)
  .settings(crossSbt)
  .settings(
    name := "sbt-giter8-scaffold",
    description := "sbt plugin for scaffolding giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala210, scala212),
    scriptedSettings,
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted := ScriptedPlugin.scripted
      .dependsOn(publishLocal in lib)
      .evaluated,
    test in Test := {
      scripted.toTask("").value
    }
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(BintrayPublish)
  .dependsOn(lib)
  .settings(crossSbt)
  .settings(
    name := "sbt-giter8",
    scriptedSettings,
    description := "sbt plugin for testing giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala210, scala212),
    resolvers += Resolver.typesafeIvyRepo("releases"),
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted := ScriptedPlugin.scripted
      .dependsOn(publishLocal in lib)
      .evaluated,
    libraryDependencies += {
      val crossSbtVersion = (sbtVersion in pluginCrossBuild).value

      val artifact = 
        partialVersion(crossSbtVersion) match {
          case Some((0, 13)) =>
            "org.scala-sbt" % "scripted-plugin"
          case Some((1, _)) =>
            "org.scala-sbt" %% "scripted-plugin"
          case _ =>
            throw new Exception(s"unexpected sbt version: $crossSbtVersion (supported: 0.13.x or 1.X)")
        }

      artifact % crossSbtVersion
    },
    test in Test := {
      scripted.toTask("").value
    }
  )

lazy val lib = (project in file("library"))
  .disablePlugins(BintrayPlugin)
  .enablePlugins(SonatypePublish)
  .settings(crossSbt)
  .settings(
    name := "giter8-lib",
    description := "shared library for app and plugin",
    crossScalaVersions := List(scala210, scala211, scala212),
    libraryDependencies ++= Seq(
      scalasti,
      jgit,
      commonsIo,
      plexusArchiver,
      gigahorse,
      scalacheck % Test,
      sbtIo % Test,
      scalatest % Test,
      scalamock % Test,
      "org.slf4j" % "slf4j-simple" % "1.7.12" % Test
    ) ++
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          Seq(scalaXml, parserCombinator)
        case _ => Nil
      })
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
