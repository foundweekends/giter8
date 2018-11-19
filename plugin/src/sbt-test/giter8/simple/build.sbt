enablePlugins(ScriptedPlugin)

scriptedBufferLog in (Test, g8) := false

TaskKey[Unit]("writeInvalidFile") := {
  IO.write(file("src/main/g8/src/test/scala/invalid.scala"), "invalid file")
}

val javaVmArgs: List[String] = {
  import scala.collection.JavaConverters._
  java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
}

g8ScriptedCompat.scriptedLaunchOpts ++= javaVmArgs.filter(
  a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
)
