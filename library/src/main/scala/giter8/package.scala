import scala.util.Try

/*
 * Original implementation (C) 2014-2015 Kenji Yoshida and contributors
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

package object giter8 {

  type VersionE = Either[String, String]

  implicit class OptTry[A](a: Try[Option[A]]) {
    def or(b: Try[Option[A]]): Try[Option[A]] =
      a.flatMap {
        unwrapped =>
          if (unwrapped.isDefined) a else b
      }
  }

  implicit class Tap[A](a: A) {
    def tap[B](block: A => B): A = {
      block(a)
      a
    }
  }
}

