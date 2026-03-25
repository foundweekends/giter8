Compile / unmanagedSourceDirectories += baseDirectory.value

// Scripted smoke-test projects must declare a Scala version.
// This fixture stores Scala sources at the project root.
scalaVersion := "2.13.14"
