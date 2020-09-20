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

sealed trait Ref
object Ref {
  case class Tag(name: String) extends Ref
  case class Branch(name: String) extends Ref
}

case class Config(
    repo: String,
    ref: Option[Ref] = None,
    forceOverwrite: Boolean = false,
    directory: Option[String] = None,
    out: Option[String] = None,
    knownHosts: Option[String] = None
)
