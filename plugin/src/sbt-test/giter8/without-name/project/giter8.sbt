addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8" % System.getProperty("plugin.version"))

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
