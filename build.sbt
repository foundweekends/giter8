import Dependencies._
import scala.sys.process.ProcessLogger

val g8version = "0.18.0-SNAPSHOT"

val javaVmArgs: List[String] = {
  import scala.collection.JavaConverters._
  java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
}

val coursierBootstrap      = taskKey[File]("create bootstrap jar")
val coursierBootstrapBatch = taskKey[File]("create bootstrap jar")
val coursierBootstrapTest  = taskKey[Unit]("test bootstrap jar")

val sbtLauncherVersion = settingKey[String]("")

ThisBuild / organization := "org.foundweekends.giter8"
ThisBuild / version := g8version
ThisBuild / scalaVersion := scala212
ThisBuild / organizationName := "foundweekends"
ThisBuild / organizationHomepage := Some(url("https://foundweekends.org/"))
ThisBuild / Compile / packageBin / publishArtifact := true
ThisBuild / homepage := Some(url("https://www.foundweekends.org/giter8/"))
ThisBuild / publishMavenStyle := true
ThisBuild / Test / publishArtifact := false
ThisBuild / Test / parallelExecution := false
ThisBuild / licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / developers := List(
  Developer("n8han", "Nathan Hamblen", "@n8han", url("https://github.com/n8han")),
  Developer("eed3si9n", "Eugene Yokota", "@eed3si9n", url("https://github.com/eed3si9n"))
)
ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/foundweekends/giter8"), "git@github.com:foundweekends/giter8.git")
)

lazy val commonSettings = Def.settings(
  scalacOptions ++= Seq("-deprecation"),
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Nil
      case _ =>
        Seq("-Xlint", "-release:8")
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Nil
      case "2.13" =>
        Seq("-Xsource:3-cross")
      case _ =>
        Seq("-Xsource:3", "-Xfuture")
    }
  }
)

lazy val root = (project in file("."))
  .enablePlugins(G8SitePlugin, NoPublish)
  .aggregate(
    Seq(
      app,
      bootstrap,
      lib,
      scaffold,
      plugin,
      gitsupport,
      launcher
    ).flatMap(_.projectRefs) *
  )
  .settings(
    commonSettings,
    crossScalaVersions := Nil,
    publish / skip := true,
    customCommands
  )

lazy val app = (projectMatrix in file("app"))
  .enablePlugins(SonatypePublish)
  .defaultAxes(VirtualAxis.jvm)
  .dependsOn(lib, gitsupport)
  .settings(
    commonSettings,
    description := "Command line tool to apply templates defined on GitHub",
    name := "giter8",
    csRun / sourceDirectory := {
      (baseDirectory).value.getParentFile / "src" / "main" / "conscript"
    },
    libraryDependencies ++= List(
      launcherIntf,
      slf4jsimple
    )
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212, scala213, scala3)
  )

lazy val crossSbt = Seq(
  pluginCrossBuild / sbtVersion := {
    scalaBinaryVersion.value match {
      case "2.12" => sbt1
      case _      => "2.0.0-RC8"
    }
  }
)

lazy val scaffold = (projectMatrix in file("scaffold"))
  .enablePlugins(SbtPlugin, SonatypePublish)
  .defaultAxes(VirtualAxis.jvm)
  .dependsOn(lib)
  .settings(crossSbt)
  .settings(
    commonSettings,
    name := "sbt-giter8-scaffold",
    description := "sbt plugin for scaffolding giter8 templates",
    sbtPlugin := true,
    scriptedLaunchOpts ++= javaVmArgs.filter(a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value)
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212, scala3)
  )

lazy val plugin = (projectMatrix in file("plugin"))
  .enablePlugins(SbtPlugin, SonatypePublish)
  .defaultAxes(VirtualAxis.jvm)
  .dependsOn(lib)
  .settings(crossSbt)
  .settings(
    commonSettings,
    name := "sbt-giter8",
    description := "sbt plugin for testing giter8 templates",
    sbtPlugin := true,
    scriptedLaunchOpts ++= javaVmArgs.filter(a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value)
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212, scala3)
  )

lazy val gitsupport = (projectMatrix in file("cli-git"))
  .enablePlugins(BuildInfoPlugin, SonatypePublish)
  .defaultAxes(VirtualAxis.jvm)
  .settings(
    commonSettings,
    description := "cli and git support library for Giter8",
    name := "giter8-cli-git",
    sbtLauncherVersion := launcherIntf.revision,
    libraryDependencies ++= Seq(
      scopt,
      jgit,
      jgitSshApache,
      commonsIo,
      scalamock % Test
    ),
    libraryDependencies ++= scalatest,
    run / fork := true,
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion, scalaBinaryVersion, sbtLauncherVersion),
    buildInfoPackage := "giter8"
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212, scala213, scala3)
  )

lazy val lib = (projectMatrix in file("library"))
  .enablePlugins(SonatypePublish)
  .defaultAxes(VirtualAxis.jvm)
  .dependsOn(gitsupport)
  .settings(crossSbt)
  .settings(
    commonSettings,
    name := "giter8-lib",
    description := "shared library for app and plugin",
    libraryDependencies ++= scalatest,
    libraryDependencies ++= Seq(
      stringTemplate,
      jgit,
      slf4jsimple,
      commonsIo,
      scalaXml,
      parserCombinator(scalaVersion.value),
      scalacheck % Test,
      sbtIo % Test,
      scalamock % Test
    ),
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "1000", "-workers", "10")
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212, scala213, scala3)
  )

lazy val launcher = (projectMatrix in file("launcher"))
  .enablePlugins(SonatypePublish)
  .enablePlugins(ConscriptPlugin)
  .defaultAxes(VirtualAxis.jvm)
  .dependsOn(gitsupport)
  .settings(
    commonSettings,
    description := "Command line tool to apply templates defined on GitHub",
    name := "giter8-launcher",
    libraryDependencies ++= Seq(
      coursier,
      slf4jsimple,
      verify % Test,
      sbtIo % Test
    ),
    testFrameworks += new TestFramework("verify.runner.Framework"),
    run / fork := true,
    Test / fork := true,
    Test / javaOptions ++= Seq(s"""-DG8_HOME=${target.value / "home"}""")
    // assemblyMergeStrategy in assembly := {
    //   case "plugin.properties" => MergeStrategy.concat
    //   case "module-info.class" => MergeStrategy.discard
    //   case x =>
    //     val oldStrategy = (assemblyMergeStrategy in assembly).value
    //     oldStrategy(x)
    // },
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212, scala213, scala3)
  )

lazy val bootstrap = (projectMatrix in file("bootstrap"))
  .enablePlugins(SonatypePublish)
  .defaultAxes(VirtualAxis.jvm)
  .settings(
    commonSettings,
    description := "Bootstrap script for Giter8 launcher",
    name := "giter8-bootstrap",
    libraryDependencies += Dependencies.coursierCli % Test,
    coursierBootstrap := Def.taskDyn {
      val t = target.value / "g8"
      val v = version.value
      (Test / runMain)
        .toTask(
          Seq(
            "coursier.cli.Coursier",
            "bootstrap",
            s"org.foundweekends.giter8:giter8-launcher_3:$v",
            "--main",
            "giter8.LauncherMain",
            "-o",
            t.getAbsolutePath,
            "--bat",
            "-f"
          ).mkString(" ", " ", "")
        )
        .map((_: Unit) => t)
    }.value,
    coursierBootstrap := coursierBootstrap
      .dependsOn(Def.task {
        (launcher.jvm(Dependencies.scala3) / publishLocal).value
        (gitsupport.jvm(Dependencies.scala3) / publishLocal).value
      })
      .value,
    coursierBootstrapBatch := {
      val _ = coursierBootstrap.value
      target.value / "g8.bat"
    },
    coursierBootstrap / artifact := {
      val o = (coursierBootstrap / artifact).value
      o.withExtension("sh")
    },
    coursierBootstrapBatch / artifact := {
      val o = (coursierBootstrapBatch / artifact).value
      o.withExtension("bat")
    },
    coursierBootstrapTest := {
      case class Res(out: Seq[String], err: Seq[String], exitCoce: Int)

      def runWithLog(p: sys.process.ProcessBuilder): Res = {
        val outLog = List.newBuilder[String]
        val errLog = List.newBuilder[String]
        val res    = p
          .!(new ProcessLogger {
            override def out(s: => String): Unit = outLog += s
            override def err(s: => String): Unit = errLog += s
            override def buffer[T](f: => T)      = f
          })

        Res(outLog.result(), errLog.result(), res)
      }

      val binary = Def.taskIf {
        if (scala.util.Properties.isWin) {
          coursierBootstrapBatch.value.getAbsolutePath
        } else {
          coursierBootstrap.value.getAbsolutePath
        }
      }.value
      val res1 = runWithLog(sys.process.Process(binary))
      assert(res1.exitCoce == 1)
      assert(res1.err.contains("Error: Missing argument <template>"), res1.err)
      assert(res1.err.contains("Try --help for more information."), res1.err)
      val res2 = runWithLog(sys.process.Process(binary, Seq("--help")))
      assert(res2.exitCoce == 0)
      assert(res2.out.contains("Usage: g8 [options] <template>"))
    },
    Test / test := {
      coursierBootstrapTest.value
      (Test / test).value
    },
    addArtifact(coursierBootstrap / artifact, coursierBootstrap),
    addArtifact(coursierBootstrapBatch / artifact, coursierBootstrapBatch)
  )
  .jvmPlatform(
    scalaVersions = Seq(scala3)
  )

def customCommands: Seq[Setting[?]] = Seq(
  commands += Command.command("release") { state =>
    "clean" :: "publishSigned" :: "sonaRelease" :: "reload" :: state
  }
)
