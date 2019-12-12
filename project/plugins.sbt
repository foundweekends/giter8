addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("org.foundweekends.conscript" % "sbt-conscript" % "0.5.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
