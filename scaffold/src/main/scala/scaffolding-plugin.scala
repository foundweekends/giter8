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

  val parser: sbt.Project.Initialize[State => Parser[(File, Map[String, String])]] =
    (baseDirectory, templatesPath) { (b, t) =>

      import complete.DefaultParsers._
      import java.io.FileInputStream
      
      (state: State) =>
        val folder = b / t

        val templatesParsers = folder.listFiles
          .filter(f => f.isDirectory && !f.isHidden)
          .map(_.getName: Parser[String])
          .reduceLeft(_ | _)

        val eq: Parser[String] = "="

        Space ~> templatesParsers.map(folder / _).flatMap { root =>

          val validParam = GIO.readProps(new FileInputStream(root / "default.properties"))
            .keys
            .map(x => x: Parser[String])
            .reduceLeft(_ | _)

          val p = ((validParam <~ eq): Parser[String]) ~ NotSpace

          Space ~> (p?) map { param => 
            (root, param.map(Map(_)).getOrElse(Map.empty))
          }
        }
    }

  val scafffoldTask = scaffold <<= InputTask(parser){ (templateData: TaskKey[(File, Map[String, String])]) =>
    (baseDirectory, templateData) map { (b, data) =>
      val (t, param) = data
      // TODO: how to handle packages names in java (folder structure)
      // Template hooks? package(toto.tutu) like ls(,,)
      // G8Helpers.applyRaw(t, b, param).fold(
      //         e => error(e),
      //         r => println("Success :)")
      //       )
    }
  }

  lazy val scaffoldSettings: Seq[sbt.Project.Setting[_]] = Seq(
    templatesPath := ".g8",
    scafffoldTask
  )
}
