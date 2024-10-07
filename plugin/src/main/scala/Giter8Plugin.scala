/*
 * Original implementation (C) 2010-2015 Nathan Hamblen and contributors
 * Adapted and extended in 2016 by foundweekends project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package giter8

import sbt._
import sbt.Path.relativeTo
import sbt.sbtgiter8.ScriptedCompat
import sbt.ScriptedPlugin.autoImport.scriptedBufferLog
import sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts
import sbt.ScriptedPlugin.autoImport.scriptedDependencies
import sbt.ScriptedPlugin.autoImport.sbtTestDirectory

object Giter8Plugin extends sbt.AutoPlugin {
  override val requires = sbt.plugins.JvmPlugin
  override val trigger  = allRequirements

  import Keys._

  object autoImport {
    @deprecated("will be removed")
    object g8ScriptedCompat extends ScriptedCompat

    lazy val g8               = taskKey[Seq[File]]("Apply default parameters to input templates and write to output.")
    lazy val g8PropertiesFile = settingKey[File]("g8-properties-file")
    lazy val g8Properties     = taskKey[Map[String, String]]("g8-properties")
    lazy val g8TestScript     = settingKey[File]("g8-test-script")
    lazy val g8Test           = inputKey[Unit]("Run `sbt test` in output to smoke-test the templates")
  }

  import autoImport._

  override lazy val globalSettings = Seq(
    scriptedBufferLog := true,
    scriptedLaunchOpts := Seq()
  )

  lazy val baseGiter8Settings: Seq[Def.Setting[?]] = Seq(
    g8 := {
      val base  = (g8 / unmanagedSourceDirectories).value
      val srcs  = (g8 / sources).value
      val out   = (g8 / target).value
      val props = (g8 / g8Properties).value
      val s     = streams.value
      IO.delete(out)
      val retval = G8(srcs pair relativeTo(base), out, props)

      // copy scaffolds
      val scaffoldsDir = (Compile / sourceDirectory).value / "scaffolds"
      val scaffolds = if (scaffoldsDir.exists) {
        val outDir = out / ".g8"
        IO.copyDirectory(scaffoldsDir, outDir)
        sbt.Path.allSubpaths(outDir).collect { case (f, _) if f.isFile => f }
      } else Nil

      retval ++ scaffolds
    },
    g8 / aggregate := false,
    g8 / unmanagedSourceDirectories := {
      val dir1 = (sourceDirectory.value / "g8").get
      if (dir1.nonEmpty) dir1
      else List(baseDirectory.value)
    },
    g8 / sources := {
      val dirs = (g8 / unmanagedSourceDirectories).value
      val root = dirs.head
      G8.templateFiles(root, baseDirectory.value)
    },
    g8 / target := { target.value / "g8" },
    g8 / g8PropertiesFile := {
      val propertiesLoc0 = ((g8 / unmanagedSourceDirectories).value / "default.properties").get.headOption
      val propertiesLoc1: Option[File] =
        Some((LocalRootProject / baseDirectory).value / "project" / "default.properties")
      (propertiesLoc0 orElse propertiesLoc1).get
    },
    g8 / g8Properties := {
      val f = (g8 / g8PropertiesFile).value
      if (f.exists) {
        val in = new java.io.FileInputStream(f)
        try {
          G8.transformProps(G8.readProps(in))
            .fold(
              err => sys.error(err),
              _.foldLeft(G8.ResolvedProperties.empty) { case (resolved, (k, v)) =>
                resolved + (k -> G8.DefaultValueF(v)(resolved))
              }.toMap
            )
        } finally {
          in.close
        }
      } else Map.empty
    }
  )

  lazy val giter8TestSettings: Seq[Def.Setting[?]] = ScriptedPlugin.projectSettings ++
    Seq(
      Test / g8Test := { ScriptedPlugin.autoImport.scripted.evaluated },
      Test / g8Test / aggregate := false,
      scriptedDependencies := {
        val x = (Test / g8).value
      },
      Test / g8 := {
        val base  = (Compile / g8 / unmanagedSourceDirectories).value
        val srcs  = (Compile / g8 / sources).value
        val out   = (Test / g8 / target).value
        val props = (Test / g8 / g8Properties).value
        val ts    = (Test / g8 / g8TestScript).value
        val s     = streams.value
        IO.delete(out)
        val retval = G8(srcs pair relativeTo(base), out, props)

        // copy scaffolds
        val scaffoldsDir = (Compile / sourceDirectory).value / "scaffolds"
        val scaffolds = if (scaffoldsDir.exists) {
          val outDir = out / ".g8"
          IO.copyDirectory(scaffoldsDir, outDir)
          sbt.Path.allSubpaths(outDir).collect { case (f, _) if f.isFile => f }
        } else Nil

        // copy test script or generate one
        // the final script should always be called "test.script"
        // no matter how it was originally called by user
        val script = new File(out, "test.script")
        if (ts.exists) IO.copyFile(ts, script)
        else IO.write(script, """>test""")

        retval ++ scaffolds :+ script
      },
      sbtTestDirectory := { target.value / "sbt-test" },
      Test / g8 / target := { sbtTestDirectory.value / name.value / "scripted" },
      g8TestScript := {
        val dir     = (Test / sourceDirectory).value
        val metadir = (LocalRootProject / baseDirectory).value / "project"
        val file0   = dir / "g8" / "test"

        // we should only use file0 if its an exisiting file
        // if it exists and is a dir we should fallback to test.script
        val defaultTestScript =
          if (file0.isDirectory) dir / "g8" / "test.script"
          else file0

        val files = List(
          file0,
          dir / "g8" / "test.script",
          dir / "g8" / "giter8.test",
          dir / "g8" / "g8.test",
          metadir / "test",
          metadir / "test.script",
          metadir / "giter8.test",
          metadir / "g8.test"
        )

        files
          .find(_.isFile)
          .getOrElse(defaultTestScript)
      },
      Test / g8 / scriptedBufferLog := true
    )

  override lazy val projectSettings: Seq[Def.Setting[?]] = inConfig(Compile)(baseGiter8Settings) ++ giter8TestSettings
}
