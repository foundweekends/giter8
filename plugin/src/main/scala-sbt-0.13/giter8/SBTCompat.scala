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

private[giter8] object SBTCompat extends ScriptedCompat

trait ScriptedCompat {
  val scriptedSettings     = sbt.ScriptedPlugin.scriptedSettings
  val sbtLauncher          = sbt.ScriptedPlugin.sbtLauncher
  val sbtTestDirectory     = sbt.ScriptedPlugin.sbtTestDirectory
  val scripted             = sbt.ScriptedPlugin.scripted
  val scriptedBufferLog    = sbt.ScriptedPlugin.scriptedBufferLog
  val scriptedClasspath    = sbt.ScriptedPlugin.scriptedClasspath
  val scriptedDependencies = sbt.ScriptedPlugin.scriptedDependencies
  val scriptedLaunchOpts   = sbt.ScriptedPlugin.scriptedLaunchOpts
  val scriptedRun          = sbt.ScriptedPlugin.scriptedRun
  val scriptedSbt          = sbt.ScriptedPlugin.scriptedSbt
  val scriptedTask         = sbt.ScriptedPlugin.scriptedTask
  val scriptedTests        = sbt.ScriptedPlugin.scriptedTests
}