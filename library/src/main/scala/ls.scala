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

import scala.util.parsing.combinator._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Parse any ls.implcit.ly references in default properties. The latest
 * version may be requested by setting a property's value to
 * 
 *   ls(library, user, repo)
 *
 * The second two parameters are optional.
 */
object Ls extends JavaTokenParsers {
  def spec =
    "ls" ~> "(" ~> word ~ optElem ~ optElem <~ ")" ^^ {
      case library ~ user ~ repo => (library, user, repo)
    }
  def optElem = opt("," ~> word)
  /** Like ident but allow hyphens */
  def word = """[\w\-]+""".r

  def unapply(value: String) =
    Some(parse(spec, value)).filter { _.successful }.map { _.get }

  def lookup(rawDefaults: G8.OrderedProperties)
  : Either[String, G8.OrderedProperties] = {
    val lsDefaults = rawDefaults.view.collect {
      case (key, Ls(library, user, repo)) =>
        ls.DefaultClient {
          _.Handler.latest(library, user, repo)
        }.right.map { future =>
          Await.result(future, 1.minute).right.map(key -> _)
        }.joinRight
    }
    val initial: Either[String,G8.OrderedProperties] = Right(rawDefaults)
    (initial /: lsDefaults) { (accumEither, lsEither) =>
      for {
        cur <- accumEither.right
        ls <- lsEither.right
      } yield {
        // Find the match in the accumulator and replace it with the ls'd value
        val (inits, tail) = cur.span { case (k, _) => k != ls._1 }
        inits ++ (ls +: (tail.tail))
      }
    }.left.map { "Error retrieving ls version info: " + _ }
  }
}
