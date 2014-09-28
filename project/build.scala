import sbt._

object Builds extends sbt.Build {
  import Keys._
  import ls.Plugin.{lsSettings,LsKeys}
  import sbtbuildinfo.Plugin._
  import conscript.Harness.conscriptSettings

  val g8version = "0.6.7-SNAPSHOT"

  val typesafeRepo = "Typesafe repo" at "http://repo.typesafe.com/typesafe/repo/"
  lazy val buildSettings = Defaults.defaultSettings ++ lsSettings ++ Seq(
    organization := "net.databinder.giter8",
    version := g8version,
    scalaVersion := "2.10.2",
    scalacOptions ++= Seq("-language:_", "-deprecation", "-Xlint"),
    libraryDependencies ++= Seq(
      "org.clapper" %% "scalasti" % "2.0.0",
      ("jline" % "jline" % "1.0" force)
    ),
    publishArtifact in (Compile, packageBin) := true,
    homepage :=
      Some(url("https://github.com/n8han/giter8")),
    publishMavenStyle := true,
    publishTo :=
      Some("releases" at
           "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
    publishArtifact in Test := false,
    licenses := Seq("LGPL v3" -> url("http://www.gnu.org/licenses/lgpl.txt")),
    pomExtra := (
      <scm>
        <url>git@github.com:n8han/giter8.git</url>
        <connection>scm:git:git@github.com:n8han/giter8.git</connection>
      </scm>
      <developers>
        <developer>
          <id>n8han</id>
          <name>Nathan Hamblen</name>
          <url>http://twitter.com/n8han</url>
        </developer>
      </developers>)
  )

  // posterous title needs to be giter8, so both app and root are named giter8
  lazy val root = Project("root", file("."),
    settings = buildSettings ++ Seq(
      name := "giter8",
      LsKeys.skipWrite := true
    )) aggregate(app, lib, scaffold, plugin)

  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ conscriptSettings ++ buildInfoSettings ++ Seq(
      description :=
        "Command line tool to apply templates defined on github",
      name := "giter8",
      libraryDependencies ++= Seq(
        "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r",
        "com.github.scopt" %% "scopt" % "3.1.0",
        "net.databinder" %% "dispatch-json" % "0.8.10",
        "net.databinder" %% "dispatch-http" % "0.8.10",
        "org.scala-sbt" % "launcher-interface" % sbtVersion.value % "provided"
      ),
      sourceGenerators in Compile <+= buildInfo,
      buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "giter8",
      resolvers += typesafeRepo
    )) dependsOn (lib)

  lazy val scaffold = Project("giter8-scaffold", file("scaffold"),
    settings = buildSettings ++ Seq(
      description := "sbt plugin for scaffolding giter8 templates",
      sbtPlugin := true
    )) dependsOn (lib)

  lazy val plugin: Project = Project("giter8-plugin", file("plugin"),
    settings = buildSettings ++ ScriptedPlugin.scriptedSettings ++ Seq(
      description := "sbt plugin for testing giter8 templates",
      sbtPlugin := true,
      resolvers ++= Seq(
        Resolver.url("Typesafe repository", url("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns),
        typesafeRepo
      ),
      ScriptedPlugin.scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
        a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
      ),
      ScriptedPlugin.scriptedBufferLog := false,
      ScriptedPlugin.scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
      ScriptedPlugin.scripted <<= ScriptedPlugin.scripted dependsOn(publishLocal in lib),
      libraryDependencies <+= sbtVersion("org.scala-sbt" % "scripted-plugin" % _)
    )) dependsOn (lib)

  lazy val lib = Project("giter8-lib", file("library"),
    settings = buildSettings ++ Seq(
      description :=
        "shared library for app and plugin",
      libraryDependencies ++= Seq(
        "me.lessis" %% "ls" % "0.1.3",
        // override ls's older version of dispatch
        "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
        "commons-io" % "commons-io" % "2.4",
        "org.codehaus.plexus" % "plexus-archiver" % "2.2" excludeAll(
          ExclusionRule("org.apache.commons", "commons-compress"),
          ExclusionRule("classworlds", "classworlds"),
          ExclusionRule("org.tukaani", "xz"),
          ExclusionRule("junit", "junit")
        )
      )
    ))
}
