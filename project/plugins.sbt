resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
