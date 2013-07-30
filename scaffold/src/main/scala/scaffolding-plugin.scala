package giter8

import sbt._

object ScaffoldPlugin extends sbt.Plugin {
  import Keys._
  import scala.io.Source

  object ScaffoldingKeys {
    lazy val templatesPath = SettingKey[String]("g8-templates-path")
    lazy val scaffold    = InputKey[Unit]("g8-scaffold",
      """g8-scaffold add code generation abilities to giter8, after a project has been generated.
         |
         |Usage:
         | g8-scaffold <scaffold_name>
         |
         |The name of the scaffold is the name of the folder located directly under `.g8`
         |Assuming you `.g8` folder has the following structure:
         |
         |    .g8
         |    |_ model
         |    |_ view
         |    |_ controller
         |
         |You have 3 different scaffodings available.
         |
         |To generate a new template, just type `g8-scaffold model`.
         |As usual, g8 will ask for the variable values, and generate the correct code.
         |""".stripMargin)
  }

  import ScaffoldingKeys._
  import complete._
  import complete.DefaultParsers._

  val parser: sbt.Project.Initialize[State => Parser[String]] =
    (baseDirectory, templatesPath) { (b, t) =>
      (state: State) =>
      val folder = b / t
      val templates = Option(folder.listFiles).toList.flatten
        .filter(f => f.isDirectory && !f.isHidden)
        .map(_.getName: Parser[String])

      (Space+) ~> templates.foldLeft("": Parser[String])(_ | _)
    }

  val scafffoldTask = scaffold <<= InputTask(parser){ (argTask: TaskKey[String]) =>
    (baseDirectory, templatesPath, argTask) map { (b, t, name) =>
      val folder = b / t
      G8Helpers.applyRaw(folder / name, b, Nil, false).fold(
        e => sys.error(e),
        r => println("Success :)")
      )
    }
  }


  lazy val scaffoldSettings: Seq[sbt.Project.Setting[_]] = Seq(
    templatesPath := ".g8",
    scafffoldTask
  )
}
