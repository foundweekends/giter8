import Dependencies._
import de.heikoseeberger.sbtheader.HeaderPattern

val g8version = "0.7.3-SNAPSHOT"

lazy val headerSettings = headers := Map(
    "scala" -> (
      HeaderPattern.cStyleBlockComment,
      """|/*
         | * Original implementation (C) 2010-2015 Nathan Hamblen and contributors
         | * Adapted and extended in 2016 by foundweekends project
         | *
         | * Licensed under the Apache License, Version 2.0 (the "License");
         | * you may not use this file except in compliance with the License.
         | * You may obtain a copy of the License at
         | *
         | * http://www.apache.org/licenses/LICENSE-2.0
         | *
         | * Unless required by applicable law or agreed to in writing, software
         | * distributed under the License is distributed on an "AS IS" BASIS,
         | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         | * See the License for the specific language governing permissions and
         | * limitations under the License.
         | */
         |
         |""".stripMargin
    )
)

// posterous title needs to be giter8, so both app and root are named giter8
lazy val root = (project in file(".")).
  enablePlugins(TravisSitePlugin, NoPublish).
  aggregate(app, lib, scaffold, plugin).
  settings(
    inThisBuild(List(
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
      licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      developers := List(
        Developer("n8han", "Nathan Hamblen", "@n8han", url("http://github.com/n8han")),
        Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
      ),
      scmInfo := Some(ScmInfo(url("https://github.com/foundweekends/giter8"), "git@github.com:foundweekends/giter8.git"))
    )),
    name := "giter8",
    crossScalaVersions := List(scala210, scala211, scala212),
    siteGithubRepo := "foundweekends/giter8",
    siteEmail := { "eed3si9n" + "@" + "gmail.com" },
    customCommands
  )

lazy val app = (project in file("app")).
  disablePlugins(BintrayPlugin).
  enablePlugins(AutomateHeaderPlugin).
  enablePlugins(ConscriptPlugin, BuildInfoPlugin, SonatypePublish).
  dependsOn(lib).
  settings(
    description := "Command line tool to apply templates defined on github",
    name := "giter8",
    crossScalaVersions := List(scala210, scala211, scala212),
    sourceDirectory in csRun := { (baseDirectory).value.getParentFile / "src" / "main" / "conscript" },
    libraryDependencies ++= Seq(scopt, logback),
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "giter8",
    headerSettings
  )

lazy val scaffold = (project in file("scaffold")).
  enablePlugins(BintrayPublish).
  enablePlugins(AutomateHeaderPlugin).
  dependsOn(lib).
  settings(
    name := "sbt-giter8-scaffold",
    description := "sbt plugin for scaffolding giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala210),
    scriptedSettings,
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted := ScriptedPlugin.scripted.dependsOn(publishLocal in lib).evaluated,
    test in Test := {
      scripted.toTask("").value
    },
    headerSettings,
    excludes := Seq("src/main/scala/ScafoldPlugin.scala")
  )

lazy val plugin = (project in file("plugin")).
  enablePlugins(BintrayPublish).
  enablePlugins(AutomateHeaderPlugin).
  dependsOn(lib).
  settings(
    name := "sbt-giter8",
    scriptedSettings,
    description := "sbt plugin for testing giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala210),
    resolvers += Resolver.typesafeIvyRepo("releases"),
    scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
      a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
    ),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted := ScriptedPlugin.scripted.dependsOn(publishLocal in lib).evaluated,
    libraryDependencies += ("org.scala-sbt" % "scripted-plugin" % sbtVersion.value),
    test in Test := {
      scripted.toTask("").value
    },
    headerSettings,
    excludes := Seq("src/main/scala/gio.scala")
  )

lazy val lib = (project in file("library")).
  disablePlugins(BintrayPlugin).
  enablePlugins(AutomateHeaderPlugin).
  enablePlugins(SonatypePublish).
  settings(
    name := "giter8-lib",
    description := "shared library for app and plugin",
    crossScalaVersions := List(scala210, scala211, scala212),
    libraryDependencies ++= Seq(
      scalasti, jgit, commonsIo, plexusArchiver,
      scalacheck % Test, sbtIo % Test, scalatest % Test,
      scalamock % Test, "org.slf4j" % "slf4j-simple" % "1.7.12" % Test
    ) ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        Seq(scalaXml, parserCombinator)
      case _ => Nil
      }),
    headerSettings,
    excludes := Seq(
      "src/main/scala/maven.scala",
      "src/main/scala/ScalastiHelper.scala")
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
