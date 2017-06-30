scriptedBufferLog in (Test, g8) := false

ScriptedPlugin.scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
  a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
)
