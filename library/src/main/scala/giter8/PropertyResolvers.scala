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

import java.io.{File, FileInputStream}
import java.util.Properties
import java.util.logging.Logger

import giter8.StringRenderer.ParameterNotFoundError

import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

trait PropertyResolver {
  protected def resolveImpl(old: Map[String, String]): Try[Map[String, String]]

  def resolve(old: Map[String, String]): Try[Map[String, String]] = resolveImpl(old).flatMap(crossResolve)

  private def crossResolve(properties: Map[String, String]): Try[Map[String, String]] = {
    var needAnotherPass = true
    var resolved        = properties

    while (needAnotherPass) {
      needAnotherPass = false
      resolved = resolved.map {
        case (name, oldValue) =>
          StringRenderer.render(oldValue, properties) match {
            case Success(newValue) =>
              if (newValue != oldValue) needAnotherPass = true
              name -> newValue
            case Failure(ParameterNotFoundError(_)) => name -> oldValue
            case Failure(t)                         => return Failure(t)
          }
      }
    }
    Success(resolved)
  }
}

object PropertyResolver {
  class PropertyResolvingError(message: String) extends RuntimeException(message)

  case class DuplicatePropertyError(names: String*)
      extends PropertyResolvingError(s"Duplicate properties: ${names.mkString(",")}")

  def mergeProperties(oldProperties: Map[String, String],
                      newProperties: Map[String, String]): Try[Map[String, String]] = Try {
    val duplicates = newProperties.keySet & oldProperties.keySet
    if (duplicates.nonEmpty) throw DuplicatePropertyError(duplicates.toSeq: _*)
    oldProperties ++ newProperties
  }
}

case class PropertyResolverChain(propertyResolvers: PropertyResolver*) extends PropertyResolver {
  override def resolveImpl(old: Map[String, String]): Try[Map[String, String]] = propertyResolvers.foldLeft(Try(old)) {
    case (properties, resolver) => properties.flatMap(resolver.resolve)
  }
}

case class FilePropertyResolver(files: File*) extends PropertyResolver {
  import PropertyResolver._

  private val log = Logger.getLogger("giter8.FilePropertyResolver")

  override protected def resolveImpl(old: Map[String, String]): Try[Map[String, String]] = {
    files.foldLeft(Try(old)) {
      case (properties, file) =>
        for {
          oldProps <- properties
          newProps <- readProperties(file)
          merged   <- mergeProperties(oldProps, newProps)
        } yield merged
    }
  }

  private def readProperties(file: File): Try[Map[String, String]] = Try {
    val properties = new Properties()
    val is         = new FileInputStream(file)
    try properties.load(is)
    finally is.close()

    var result = mutable.Map.empty[String, String]
    properties.stringPropertyNames.asScala.foreach { name =>
      if (result.contains(name)) throw DuplicatePropertyError(name)

      val value = properties.getProperty(name)

      // Workaround for https://github.com/foundweekends/giter8/issues/170.
      // We notify user about deprecated call to implicit.ly.
      val LsCall = """ls\((.*),(.*)\)""".r
      value match {
        case LsCall(group, artifact) =>
          log.warning(
            s"ls() function is deprecated. " +
              s"Use maven() function to get latest version of ${group.trim}#${artifact.trim}.")
        case _ => ()
      }

      result += name -> value
    }
    result.toMap
  }
}

object InteractivePropertyResolver extends PropertyResolver {
  override protected def resolveImpl(old: Map[String, String]): Try[Map[String, String]] = Try {
    old.get("description").foreach { description =>
      println(description + "\n")
    }

    val result = mutable.Map.empty[String, String]
    old.foreach {
      case (name, value) =>
        if (name != "verbatim" || name != "description") {

          // Workaround for https://github.com/foundweekends/giter8/issues/170.
          // We ask user to enter dependency version manually.
          val LsCall = """ls\((.*),(.*)\)""".r
          value match {
            case LsCall(group, artifact) =>
              print(s"$name [Please, enter the ${group.trim}#${artifact.trim} version]: ")
            case _ => print(s"$name [$value]: ")
          }
          Console.flush() // Gotta flush for Windows console!
          Option(Console.readLine()) match {
            case Some(in) if in.trim.nonEmpty => result += name -> in.trim
            case _                            => result += name -> value
          }
        }
    }
    result.toMap
  }
}

case class StaticPropertyResolver(additionalProperties: Map[String, String]) extends PropertyResolver {
  override protected def resolveImpl(old: Map[String, String]): Try[Map[String, String]] = Success {
    old ++ additionalProperties
  }
}
