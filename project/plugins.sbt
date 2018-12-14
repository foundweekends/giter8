addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("org.foundweekends.conscript" % "sbt-conscript" % "0.5.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
