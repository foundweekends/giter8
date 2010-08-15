import sbt._

class Giter8Project(info: ProjectInfo) extends DefaultProject(info) {
  val launchInterface = "org.scala-tools.sbt" % "launcher-interface" % "0.7.4" % "provided"

  val dispatch = "net.databinder" %% "dispatch-lift-json" % "0.7.5"

  val clapperOrgRepo = "clapper.org Maven Repository" at "http://maven.clapper.org"
  val scalasti = "org.clapper" %% "scalasti" % "0.4.1" intransitive()
  val stringTemplate = "org.antlr" % "stringtemplate" % "3.2.1"
}
