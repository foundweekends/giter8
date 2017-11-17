scriptedBufferLog in (Test, g8) := false

g8ScriptedCompat.scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
  a => Seq("-Xmx", "-Xms", "-XX").exists(a.startsWith)
)
