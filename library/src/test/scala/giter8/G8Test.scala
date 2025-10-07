package giter8

import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import sbt.io.IO
import sbt.io.syntax._

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import scala.collection.JavaConverters._

object G8Test extends Properties("G8") {
  private implicit val stringArbitrary: Arbitrary[String] = Arbitrary(Gen.alphaNumStr)

  property("copy with permissions") = Prop.forAll {
    (additionalPermissions: Seq[PosixFilePermission], content: String) =>
      if (scala.util.Properties.isWin) {
        true
      } else {
        val permissions = PosixFilePermission.OWNER_READ +: additionalPermissions

        IO.withTemporaryDirectory { dir =>
          val in  = dir / "g8-test-in"
          val out = dir / "g8-test-out"
          Files.write(in.toPath, content.getBytes(StandardCharsets.UTF_8))
          Files.setPosixFilePermissions(in.toPath, permissions.toSet.asJava)
          G8.write(in = in, out = out, parameters = Map.empty)
          val actual = Files.getPosixFilePermissions(out.toPath)
          val expect = permissions.toSet.asJava
          assert(actual == expect, (actual, expect))
          Files.readAllLines(in.toPath, StandardCharsets.UTF_8) == Files.readAllLines(
            out.toPath,
            StandardCharsets.UTF_8
          )
        }
      }
  }
}
