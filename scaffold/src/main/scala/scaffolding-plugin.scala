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

        val folders = folder.listFiles.filter(f => f.isDirectory && !f.isHidden)
        val templatesParsers = folders
          .map(_.getName: Parser[String])
          .reduceLeft(_ | _)


        val ex = for(
          f <- folders;
          k <- GIO.readProps(new FileInputStream(f / "default.properties")).keys.toList
        ) yield (k)

        val eq: Parser[String] = "="
        
        Space ~> templatesParsers.map(folder / _).flatMap { root =>

          val params = GIO.readProps(new FileInputStream(root / "default.properties"))
          val validParam = params
            .keys
            .map(x => x: Parser[String])
            .reduceLeft(_ | _)

          val p = ((validParam <~ eq): Parser[String]) ~ NotSpace

          def filtered(consumed: List[String]): Parser[Map[String, String]] = (Space ~> p).map(Map(_))
            .filter( 
              parsed => !consumed.contains(parsed.head._1),
              c => "PAF" // ?? 
            ).flatMap { case parsed =>
              (filtered(consumed :+ parsed.head._1) ?).map{
                case Some(m) => m ++ parsed
                case None => parsed
              }
            }

          filtered(Nil).map{ (root, _) }.examples(ex:_*)

          // Space ~> (p?) map { param => 
          //  (root, param.map(Map(_)).getOrElse(Map.empty))
          // }
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
