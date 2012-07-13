package giter8

import sbt._

object ScaffoldPlugin extends sbt.Plugin {
  import Keys._
  import scala.io.Source

  object ScaffoldingKeys {
    lazy val templatesPath = SettingKey[String]("g8-templates-path")
    lazy val scaffold    = InputKey[Unit]("g8-scaffold")
  }

  import ScaffoldingKeys._
  import complete._
  import complete.DefaultParsers._

  val parser: sbt.Project.Initialize[State => Parser[String]] =
    (baseDirectory, templatesPath) { (b, t) =>
      (state: State) =>
      val folder = b / t
      val templates = folder.listFiles
        .filter(f => f.isDirectory && !f.isHidden)
        .map(_.getName: Parser[String])

      Space ~> templates.reduceLeft(_ | _)
    }

  val scafffoldTask = scaffold <<= InputTask(parser){ (argTask: TaskKey[String]) =>
    (baseDirectory, templatesPath, argTask) map { (b, t, name) =>
      val folder = b / t
      G8Helpers.applyRaw(folder / name, b, Nil).fold(
        e => error(e),
        r => println("Success :)")
      )
    }
  }


  lazy val scaffoldSettings: Seq[sbt.Project.Setting[_]] = Seq(
    templatesPath := ".g8",
    scafffoldTask
  )
}
