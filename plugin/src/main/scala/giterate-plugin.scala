package giter8

import sbt._
import ScriptedPlugin._

object Plugin extends sbt.Plugin {
  import Keys._
  import scala.io.Source

  object G8Keys {
    lazy val g8                = TaskKey[Seq[File]]("g8", "Apply default parameters to input templates and write to output.")
    lazy val outputPath        = SettingKey[File]("g8-output-path")
    lazy val propertiesFile    = SettingKey[File]("g8-properties-file")
    lazy val properties        = TaskKey[Map[String, String]]("g8-properties")
    lazy val testScript        = SettingKey[File]("g8-test-script")
    lazy val g8Test            = InputKey[Unit]("g8-test", "Run `sbt test` in output to smoke-test the templates")
    lazy val g8TestBufferLog   = SettingKey[Boolean]("g8-test-buffer-log")
  }

  import G8Keys._
  
  lazy val baseGiter8Settings: Seq[Def.Setting[_]] = Seq(
    g8 <<= (unmanagedSourceDirectories in g8,
        sources in g8, outputPath in g8,
        properties in g8, streams) map { (base, srcs, out, props, s) =>
      IO.delete(out)
      G8(srcs x relativeTo(base), out, props) },
    unmanagedSourceDirectories in g8 <<= (sourceDirectory) { dir => (dir / "g8").get },
    sources in g8 <<= (unmanagedSourceDirectories in g8, propertiesFile in g8) map { (dirs, pf) =>
      ((dirs ** (-DirectoryFilter)) --- pf).get },
    outputPath in g8 <<= (target) { dir => dir / "g8" },
    propertiesFile in g8 <<= (unmanagedSourceDirectories in g8) { dirs => (dirs / "default.properties").get.head },
    properties in g8 <<= (propertiesFile in g8) map { f =>
      G8Helpers.transformProps(G8Helpers.readProps(new java.io.FileInputStream(f))).fold(
        err => sys.error(err),
        _.foldLeft(G8.ResolvedProperties.empty) { case (resolved, (k, v)) =>
          resolved + (k -> G8.DefaultValueF(v)(resolved))
        }.toMap
      )
    }
  )
  
  lazy val giter8TestSettings: Seq[Def.Setting[_]] = scriptedSettings ++ Seq(
    g8Test in Test <<= scriptedTask,
    scriptedDependencies <<= (g8 in Test) map { _ => },
    g8 in Test <<= (unmanagedSourceDirectories in g8 in Compile,
        sources in g8 in Compile, outputPath in g8 in Test,
        properties in g8 in Test, testScript in Test, streams) map { (base, srcs, out, props, ts, s) =>
      IO.delete(out)
      val retval = G8(srcs x relativeTo(base), out, props)
      
      // copy test script or generate one
      val script = new File(out, "test")
      if (ts.exists) IO.copyFile(ts, script)
      else IO.write(script, """>test""")
      retval :+ script
    },
    sbtTestDirectory <<= (target) { dir => dir / "sbt-test" },
    outputPath in g8 in Test <<= (sbtTestDirectory, name) { (dir, name) => dir / name / "scripted" },
    testScript <<= (sourceDirectory in Test) { dir => dir / "g8" / "test" },
    scriptedBufferLog <<= g8TestBufferLog,
    g8TestBufferLog := true
  )
  
  lazy val giter8Settings: Seq[Def.Setting[_]] = inConfig(Compile)(baseGiter8Settings) ++ giter8TestSettings
}
