resolvers ++= Seq(
  "Typesafe repo" at "http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/",
  Resolver.url("Typesafe repo Ivy style", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
  "coda" at "http://repo.codahale.com"
)

addSbtPlugin("net.databinder" %% "conscript-plugin" % "0.3.5")

addSbtPlugin("me.lessis" %% "ls-sbt" % "0.1.2")

addSbtPlugin("com.eed3si9n" %% "sbt-buildinfo" % "0.3.1")
