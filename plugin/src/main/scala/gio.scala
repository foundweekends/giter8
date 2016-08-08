/*
 * Original implementation (C) 2012 Kenji Yoshida
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

object GIO {

  def readProps(stm: java.io.InputStream) = {
    import scala.collection.JavaConversions._
    val p = new java.util.Properties
    p.load(stm)
    stm.close()
    (Map.empty[String, String] /: p.propertyNames) { (m, k) =>
      m + (k.toString -> p.getProperty(k.toString))
    }
  }
}
