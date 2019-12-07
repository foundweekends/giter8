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

import java.io.File

class Giter8 extends Runner with xsbti.AppMain {

  /** The launched conscript entry point */
  def run(config: xsbti.AppConfiguration): Giter8.Exit = {
    new Giter8.Exit(run(config.arguments))
  }

  def run(args: Array[String]): Int =
    run(args, new File(".").getAbsoluteFile)

  def run(args: Array[String], baseDirectory: File): Int =
    run(args, baseDirectory, new AppProcessor)
}

object Giter8 extends Giter8 {

  /** Main-class runner just for testing from sbt*/
  def main(args: Array[String]): Unit = {
    System.exit(run(args))
  }

  class Exit(val code: Int) extends xsbti.Exit
}

/**
  * The processor that is responsible for rendering.
  */
class AppProcessor extends Processor {
  def process(
      templateDirectory: File,
      workingDirectory: File,
      arguments: Seq[String],
      forceOverwrite: Boolean,
      outputDirectory: Option[File]
  ): Either[String, String] = {
    val templateRenderer = G8TemplateRenderer
    templateRenderer.render(templateDirectory, workingDirectory, arguments, forceOverwrite, outputDirectory)
  }
}
