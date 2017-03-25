name := "$name$"

description := "$description$"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.example" %% "foo_$scala_major_version$" % "$stable_foo_version$"
)
