import sbt._
import Keys._

object G8SitePlugin extends sbt.AutoPlugin {
  override lazy val projectSettings = Seq(
    TaskKey[Unit]("makeSite") := Def.uncached {
      val output = target.value / "site"
      IO.delete(output)
      val src     = (LocalRootProject / baseDirectory).value / "docs"
      val storage = pamflet.FileStorage(src, Nil)
      pamflet.Produce(storage.globalized, output)
      IO.delete(output / "offline")
      IO.delete(output / "ja" / "offline")
      IO.delete(output / "ko" / "offline")
    }
  )
}
