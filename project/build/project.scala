import sbt._

class Giter8Project(info: ProjectInfo) extends DefaultProject(info) {
  val launchInterface = "org.scala-tools.sbt" % "launcher-interface" % "0.7.4" % "provided"

  val dispatch = "net.databinder" %% "dispatch-lift-json" % "0.7.5"
}
