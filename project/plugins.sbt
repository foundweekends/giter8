addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.6.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("org.foundweekends.conscript" % "sbt-conscript" % "0.5.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
