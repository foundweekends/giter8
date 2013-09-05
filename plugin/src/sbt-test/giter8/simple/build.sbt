giter8Settings

G8Keys.g8TestBufferLog := false

TaskKey[Unit]("writeInvalidFile") := {
  IO.write(file("src/main/g8/src/test/scala/invalid.scala"), "invalid file")
}

