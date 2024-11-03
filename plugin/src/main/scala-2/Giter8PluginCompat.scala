package giter8

private[giter8] object Giter8PluginCompat {
  implicit class DefOps(private val self: sbt.Def.type) extends AnyVal {
    def uncached[A](a: A): A = a
  }
}
