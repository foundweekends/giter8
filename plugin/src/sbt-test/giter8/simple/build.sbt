scriptedBufferLog in (Test, g8) := false

TaskKey[Unit]("writeInvalidFile") := {
  IO.write(file("src/main/g8/src/test/scala/invalid.scala"), "invalid file")
}

ScriptedPlugin.scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
  a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
)

