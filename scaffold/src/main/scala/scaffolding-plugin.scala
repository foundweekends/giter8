package giter8

import sbt._

object ScaffoldPlugin extends sbt.Plugin {
  import Keys._
  import scala.io.Source

  object ScaffoldingKeys {
    lazy val templatesPath = SettingKey[String]("g8-templates-path")
    lazy val scaffold    = TaskKey[String]("g8-scaffold")
  }

  import ScaffoldingKeys._

  val scafffoldTask = scaffold <<= (baseDirectory, templatesPath) map { (b, t) =>
    val folder = b / t
    println(folder)
    "BOOM"
  }

  lazy val scaffoldSettings: Seq[sbt.Project.Setting[_]] = Seq(
    templatesPath := ".g8",
    scafffoldTask
  )
}
