enablePlugins(ScriptedPlugin)

Compile / unmanagedSourceDirectories += baseDirectory.value

// ScriptedPlugin 1.x pulls scripted-sbt for this Scala binary version (2.12 is always published).
scalaVersion := "2.12.21"
