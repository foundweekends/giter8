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

package sbt
package sbtgiter8

@deprecated("will be removed")
object SBTCompat extends ScriptedCompat

@deprecated("will be removed")
trait ScriptedCompat {
  val finalScriptName      = "test.script"
  val scriptedSettings     = sbt.ScriptedPlugin.projectSettings
  val sbtLauncher          = sbt.ScriptedPlugin.autoImport.sbtLauncher
  val sbtTestDirectory     = sbt.ScriptedPlugin.autoImport.sbtTestDirectory
  val scripted             = sbt.ScriptedPlugin.autoImport.scripted
  val scriptedBufferLog    = sbt.ScriptedPlugin.autoImport.scriptedBufferLog
  val scriptedClasspath    = sbt.ScriptedPlugin.autoImport.scriptedClasspath
  val scriptedDependencies = sbt.ScriptedPlugin.autoImport.scriptedDependencies
  val scriptedLaunchOpts   = sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts
  val scriptedRun          = sbt.ScriptedPlugin.autoImport.scriptedRun
  val scriptedSbt          = sbt.ScriptedPlugin.autoImport.scriptedSbt
  val scriptedTask         = sbt.ScriptedPlugin.autoImport.scripted
  val scriptedTests        = sbt.ScriptedPlugin.autoImport.scriptedTests
}
