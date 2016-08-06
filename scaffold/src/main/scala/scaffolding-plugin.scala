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

object ScaffoldPlugin extends sbt.Plugin {
  import Keys._
  import scala.io.Source

  object ScaffoldingKeys {
    lazy val templatesPath = SettingKey[String]("g8-templates-path")
    lazy val scaffold      = InputKey[Unit]("g8-scaffold")
  }

  import ScaffoldingKeys._
  import complete._
  import complete.DefaultParsers._

  val parser: Def.Initialize[State => Parser[String]] =
    (baseDirectory, templatesPath) { (b, t) =>
      (state: State) =>
      val folder = b / t
      val templates = Option(folder.listFiles).toList.flatten
        .filter(f => f.isDirectory && !f.isHidden)
        .map(_.getName: Parser[String])

      (Space) ~> templates.foldLeft(" ": Parser[String])(_ | _)
    }

  val scafffoldTask = scaffold <<= Def.inputTask{
    val name = parser.parsed
    val folder = baseDirectory.value / templatesPath.value
    G8Helpers.applyRaw(folder / name, baseDirectory.value, Nil, false).fold(
      e => sys.error(e),
      r => println("Success :)")
    )
  }


  lazy val scaffoldSettings: Seq[Def.Setting[_]] = Seq(
    templatesPath := ".g8",
    scafffoldTask
  )
}
