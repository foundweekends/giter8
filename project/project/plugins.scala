import sbt._

object Plugin extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn(
    uri("git://github.com/n8han/conscript-plugin.git#ff4599bcf5ca9737")
  )
}
