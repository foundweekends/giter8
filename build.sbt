import Dependencies._
import CrossVersion.partialVersion

val g8version = "0.16.2-SNAPSHOT"

val javaVmArgs: List[String] = {
  import scala.collection.JavaConverters._
  java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
}

val coursierBootstrap      = taskKey[File]("create bootstrap jar")
val coursierBootstrapBatch = taskKey[File]("create bootstrap jar")

ThisBuild / organization := "org.foundweekends.giter8"
ThisBuild / version := g8version
ThisBuild / scalaVersion := scala212
ThisBuild / organizationName := "foundweekends"
ThisBuild / organizationHomepage := Some(url("https://foundweekends.org/"))
ThisBuild / scalacOptions ++= Seq("-language:_", "-deprecation", "-Xlint", "-Xfuture")
ThisBuild / Compile / packageBin / publishArtifact := true
ThisBuild / homepage := Some(url("https://www.foundweekends.org/giter8/"))
ThisBuild / publishMavenStyle := true
ThisBuild / publishTo :=
  Some(
    "releases" at
      "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )
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

ThisBuild / commands += Command.command("SetScala213") {
  s"++ ${scala213}! -v" :: _
}

// posterous title needs to be giter8, so both app and root are named giter8
lazy val root = (project in file("."))
  .enablePlugins(TravisSitePlugin, NoPublish)
  .aggregate(app, lib, scaffold, plugin, gitsupport, launcher)
  .settings(
    name := "giter8",
    crossScalaVersions := Nil,
    siteGitHubRepo := "foundweekends/giter8",
    siteEmail := { "74864734+foundweekends-bot[bot]@users.noreply.github.com" },
    publish / skip := true,
    customCommands
  )

lazy val app = (project in file("app"))
  .enablePlugins(SonatypePublish)
  .dependsOn(lib, gitsupport)
  .settings(
    description := "Command line tool to apply templates defined on GitHub",
    name := "giter8",
    crossScalaVersions := List(scala212, scala213),
    csRun / sourceDirectory := {
      (baseDirectory).value.getParentFile / "src" / "main" / "conscript"
    },
    libraryDependencies += launcherIntf
  )

lazy val crossSbt = Seq(
  crossSbtVersions := List(sbt1),
  scalaVersion := {
    val crossSbtVersion = (pluginCrossBuild / sbtVersion).value
    partialVersion(crossSbtVersion) match {
      case Some((1, _)) => scala212
      case _ =>
        throw new Exception(s"unexpected sbt version: $crossSbtVersion (supported: 1.X)")
    }
  }
)

lazy val scaffold = (project in file("scaffold"))
  .enablePlugins(SbtPlugin, SonatypePublish)
  .dependsOn(lib)
  .settings(crossSbt)
  .settings(
    name := "sbt-giter8-scaffold",
    description := "sbt plugin for scaffolding giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala212),
    scriptedLaunchOpts ++= javaVmArgs.filter(a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted := scripted
      .dependsOn(lib / publishLocal)
      .evaluated,
    Test / test := {
      scripted.toTask("").value
    }
  )

lazy val plugin = (project in file("plugin"))
  .enablePlugins(SbtPlugin, SonatypePublish)
  .dependsOn(lib)
  .settings(crossSbt)
  .settings(
    name := "sbt-giter8",
    description := "sbt plugin for testing giter8 templates",
    sbtPlugin := true,
    crossScalaVersions := List(scala212),
    resolvers += Resolver.typesafeIvyRepo("releases"),
    scriptedLaunchOpts ++= javaVmArgs.filter(a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)),
    scriptedBufferLog := false,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scripted := scripted
      .dependsOn(lib / publishLocal)
      .evaluated,
    libraryDependencies += {
      val crossSbtVersion = (pluginCrossBuild / sbtVersion).value

      val artifact =
        partialVersion(crossSbtVersion) match {
          case Some((1, _)) =>
            "org.scala-sbt" %% "scripted-plugin"
          case _ =>
            throw new Exception(s"unexpected sbt version: $crossSbtVersion (supported: 1.X)")
        }

      artifact % crossSbtVersion
    },
    Test / test := {
      scripted.toTask("").value
    }
  )

lazy val gitsupport = (project in file("cli-git"))
  .enablePlugins(BuildInfoPlugin, SonatypePublish)
  .settings(
    description := "cli and git support library for Giter8",
    name := "giter8-cli-git",
    crossScalaVersions := List(scala212, scala213),
    libraryDependencies ++= Seq(
      scopt,
      jgit,
      jgitSshApache,
      commonsIo,
      scalamock % Test
    ),
    libraryDependencies ++= scalatest,
    run / fork := true,
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "giter8"
  )

lazy val lib = (project in file("library"))
  .enablePlugins(SonatypePublish)
  .dependsOn(gitsupport)
  .settings(crossSbt)
  .settings(
    name := "giter8-lib",
    description := "shared library for app and plugin",
    crossScalaVersions := List(scala212, scala213),
    libraryDependencies ++= scalatest,
    libraryDependencies ++= Seq(
      stringTemplate,
      jgit,
      commonsIo,
      plexusArchiver,
      scalaXml,
      parserCombinator(scalaVersion.value),
      scalacheck % Test,
      sbtIo % Test,
      scalamock % Test,
      "org.slf4j" % "slf4j-simple" % "2.0.9" % Test
    ),
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-minSuccessfulTests", "1000", "-workers", "10")
  )

lazy val launcher = (project in file("launcher"))
  .enablePlugins(SonatypePublish)
  .enablePlugins(ConscriptPlugin)
  .dependsOn(gitsupport)
  .settings(
    description := "Command line tool to apply templates defined on GitHub",
    name := "giter8-launcher",
    crossScalaVersions := List(scala212, scala213),
    libraryDependencies ++= Seq(
      coursier,
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

lazy val bootstrap = (project in file("bootstrap"))
  .enablePlugins(SonatypePublish)
  .settings(
    description := "Bootstrap script for Giter8 launcher",
    name := "giter8-bootstrap",
    coursierBootstrap := {
      val t = target.value / "g8"
      val v = version.value
      sys.process
        .Process(
          s"""coursier bootstrap org.foundweekends.giter8:giter8-launcher_2.12:$v --main giter8.LauncherMain -o $t --bat -f"""
        )
        .!
      t
    },
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
    addArtifact(coursierBootstrap / artifact, coursierBootstrap),
    addArtifact(coursierBootstrapBatch / artifact, coursierBootstrapBatch)
  )

def customCommands: Seq[Setting[_]] = Seq(
  commands += Command.command("release") { state =>
    "clean" ::
      s"++${scala213}" ::
      "lib/publishSigned" ::
      "gitsupport/publishSigned" ::
      "app/publishSigned" ::
      s"++${scala212}" ::
      s"^^${sbt1}" ::
      "lib/publishSigned" ::
      "gitsupport/publishSigned" ::
      "launcher/publishSigned" ::
      "app/publishSigned" ::
      "plugin/publishSigned" ::
      "scaffold/publishSigned" ::
      "reload" ::
      state
  }
)
