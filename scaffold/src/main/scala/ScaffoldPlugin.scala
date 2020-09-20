/*
 * Original implementation (C) 2012-2016 Julien Tournay and contributors
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
import Keys._

object ScaffoldPlugin extends sbt.AutoPlugin {
  override val requires = sbt.plugins.JvmPlugin
  override val trigger  = allRequirements

  object autoImport {
    lazy val g8ScaffoldTemplatesDirectory = settingKey[File]("Directory used to hold scaffolding templates.")
    lazy val g8Scaffold                   = inputKey[Unit]("Runs scaffolding")
  }

  import autoImport._
  import complete._
  import complete.DefaultParsers._

  val parser: Def.Initialize[State => Parser[(String, List[String])]] =
    Def.setting {
      val dir = g8ScaffoldTemplatesDirectory.value
      (state: State) =>
        val templates = Option(dir.listFiles).toList.flatten
          .filter(f => f.isDirectory && !f.isHidden)
          .map(_.getName: Parser[String])
        (Space) ~> token(templates.foldLeft(" ": Parser[String])(_ | _)).examples("<template>") ~
          (Space ~> StringBasic.examples("--k=v")).* map { case tmp ~ args =>
            (tmp, args.toList)
          }
    }

  val scaffoldTask =
    Def.inputTask {
      val (name, args) = parser.parsed
      val folder       = g8ScaffoldTemplatesDirectory.value
      G8.fromDirectoryRaw(folder / name, baseDirectory.value, args, false)
        .fold(
          e => sys.error(e),
          r => println("Success :)")
        )
    }

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    g8ScaffoldTemplatesDirectory := { baseDirectory.value / ".g8" },
    g8Scaffold := scaffoldTask.evaluated
  )
}
