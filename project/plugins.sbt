addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("org.foundweekends.conscript" % "sbt-conscript" % "0.5.9")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
libraryDependencies += "org.foundweekends" %% "pamflet-library" % "0.12.0"
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"
