addSbtPlugin("net.databinder" % "conscript-plugin" % "0.3.5")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.1.2")
