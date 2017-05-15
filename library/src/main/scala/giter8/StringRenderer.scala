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

package giter8 {

  import org.clapper.scalasti.{AttributeRenderer, STErrorListener, STGroup, STHelper}
  import org.stringtemplate.v4.compiler.STException
  import org.stringtemplate.v4.misc.{ErrorType, STMessage}

  import scala.util.{Failure, Success, Try}

  case class RenderOptions(token: Char) {
    val rgx: String = token match {
      case '$' => "\\$"
      case _ => new String(Array(token))
    }

    val formatSource = s"""${rgx}(\\w+)__(\\w+)${rgx}"""
    val formatTarget = s"""${rgx}$$1;format="$$2"${rgx}"""
    val propertyMatcher = s"""${rgx}([^\\;${rgx}]*)(;format=\\"[^${rgx}\"]*\\")?${rgx}"""
  }

  object FormatFunctions {
    lazy val renderOpts = StringRenderer.options
    def formatize(s: String): String = s.replaceAll(renderOpts.formatSource, renderOpts.formatTarget)

    def decapitalize(s: String): String = if (s.isEmpty) s else s(0).toLower + s.substring(1)
    def startCase(s: String): String    = s.toLowerCase.split(" ").map(_.capitalize).mkString(" ")
    def wordOnly(s: String): String     = s.replaceAll("""\W""", "")
    def upperCamel(s: String): String   = wordOnly(startCase(s))
    def lowerCamel(s: String): String   = decapitalize(upperCamel(s))
    def hyphenate(s: String): String    = s.replaceAll("""\s+""", "-")
    def normalize(s: String): String    = hyphenate(s.toLowerCase)
    def snakeCase(s: String): String    = s.replaceAll("""[\s\.\-]+""", "_")
    def packageDir(s: String): String   = s.replace(".", System.getProperty("file.separator"))

    def addRandomId(s: String): String = {
      val randomNumberAsString = new java.math.BigInteger(256, new java.security.SecureRandom).toString(32)
      s"$s-$randomNumberAsString"
    }
  }

  object StringRenderer {
    import FormatFunctions._

    val options: RenderOptions = {
      val token: Char = Option(System.getProperty("g8token")) match {
        case Some(str) if (str.length > 0) => str.charAt(0)
        case _ => '$'
      }
      RenderOptions(token)
    }

    class StringRenderingError(message: String) extends RuntimeException(message)
    case class ParameterNotFoundError(parameter: String)
        extends StringRenderingError(s"""Parameter "$parameter" not found""")
    case class IncorrectFormatError(format: String) extends StringRenderingError(s"""Format "$format" is invalid""")

    def formatString(value: String, formatName: String): String = formatName match {
      case "upper"    | "uppercase"       => value.toUpperCase
      case "lower"    | "lowercase"       => value.toLowerCase
      case "cap"      | "capitalize"      => value.capitalize
      case "decap"    | "decapitalize"    => decapitalize(value)
      case "start"    | "start-case"      => startCase(value)
      case "word"     | "word-only"       => wordOnly(value)
      case "Camel"    | "upper-camel"     => upperCamel(value)
      case "camel"    | "lower-camel"     => lowerCamel(value)
      case "hyphen"   | "hyphenate"       => hyphenate(value)
      case "norm"     | "normalize"       => normalize(value)
      case "snake"    | "snake-case"      => snakeCase(value)
      case "packaged" | "package-dir"     => packageDir(value)
      case "random"   | "generate-random" => addRandomId(value)
      case _ => value
    }

    def render(body: String, parameters: Map[String, String]): Try[String] = {
      val group = STGroup(options.token, options.token)
      var error: Option[Throwable] = None
      group.nativeGroup.setListener(new ErrorListener(e => error = Some(e)))
      group.registerRenderer(new StringRenderer)
      val helper = STHelper(group, body).setAttributes(parameters)
      Try(helper.render()) flatMap { result =>
        error match {
          case None    => Success(result)
          case Some(e) => Failure(e)
        }
      }
    }

    class ErrorListener(handler: Throwable => Unit) extends STErrorListener {
      def compileTimeError(msg: STMessage): Unit = handleError(msg)
      def internalError(msg: STMessage): Unit    = handleError(msg)
      def IOError(msg: STMessage): Unit          = handleError(msg)
      def runTimeError(msg: STMessage): Unit     = handleError(msg)

      private def handleError(msg: STMessage): Unit = msg.error match {
        // RUNTIME SEMANTIC ERRORS
        case ErrorType.NO_SUCH_ATTRIBUTE => handler(ParameterNotFoundError(msg.arg.toString))
        case _                           => handler(new StringRenderingError(msg.toString))
      }
    }

    class StringRenderer extends AttributeRenderer[String] {
      override def toString(value: String, formatName: String, locale: java.util.Locale): String = {
        if (formatName == null) value
        else {
          val formats = formatName.split(",").map(_.trim)
          formats.foldLeft(value)(formatString)
        }
      }
    }
  }
}

package org.clapper.scalasti {
  object STHelper {
    import org.stringtemplate.v4.{ST => _ST}
    def apply(group: STGroup, template: String): ST = new ST(new _ST(group.nativeGroup, template))
  }
}
