enablePlugins(ScriptedPlugin)

scalaVersion := "2.12.21"

TaskKey[Unit]("writeInvalidFile") := {
  IO.write(file("src/main/g8/src/test/scala/invalid.scala"), "invalid file")
}
