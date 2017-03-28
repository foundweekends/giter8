import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName    = "sample"
  val appVersion = "1.0"

  val appDependencies = Seq(
    // Add your project dependencies here,
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
    giter8.ScaffoldPlugin.scaffoldSettings: _*
  )

}
