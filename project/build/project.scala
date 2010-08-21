import sbt._

class Giter8Project(info: ProjectInfo) extends ParentProject(info) with posterous.Publish {
  lazy val app = project("app", "giter8", new DefaultProject(_) {
    val launchInterface = "org.scala-tools.sbt" % "launcher-interface" % "0.7.4" % "provided"

    val databinder_net = "databinder.net repository" at "http://databinder.net/repo"
    val dispatch = "net.databinder" %% "dispatch-lift-json" % "0.7.5"

    val clapperOrgRepo = "clapper.org Maven Repository" at "http://maven.clapper.org"
    val scalasti = "org.clapper" %% "scalasti" % "0.5"
  })
  lazy val plugin = project("plugin", "giter8 plugin", new PluginProject(_) {
    val stringTemplate = "org.antlr" % "stringtemplate" % "3.2.1"
  })
  override def postTitle(vers: String) = "%s %s".format("giter8", vers)
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
