enablePlugins(ScriptedPlugin)

scriptedBufferLog in (Test, g8) := false

val javaVmArgs: List[String] = {
  import scala.collection.JavaConverters._
  java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList
}

g8ScriptedCompat.scriptedLaunchOpts ++= javaVmArgs.filter(a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith))
