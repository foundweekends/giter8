package giter8

import org.scalacheck.{Gen, Prop, Properties}
import org.scalacheck.Prop.BooleanOperators

final class StableVersionOrderingTest extends Properties("StableVersion") {

  def genStableVersion: Gen[StableVersion] = for {
    major <- Gen.choose(0, 100)
    minor <- Gen.choose(0, 100)
    patch <- Gen.choose(0, 100)
  } yield StableVersion(major, minor, patch)

  def genStableVersionList: Gen[Seq[StableVersion]] = for {
    n        <- Gen.choose(1, 5)
    versions <- Gen.listOfN(n, genStableVersion)
  } yield versions

  property("ordering") = Prop.forAll(genStableVersionList) { (versions: Seq[StableVersion]) =>
    val sortedVersion = versions.sorted.head
    val maxMajor      = versions.maxBy(_.major).major
    val maxMinor      = versions.filter(_.major == maxMajor).maxBy(_.minor).minor
    val maxPatch      = versions.filter(v => v.major == maxMajor && v.minor == maxMinor).maxBy(_.patch).patch

    val maxMajorCount = versions.count(_.major == maxMajor)
    val maxMinorCount = versions.count(v => v.major == maxMajor && v.minor == maxMinor)
    val maxPatchCount = versions.count(v => v.major == maxMajor && v.minor == maxMinor && v.patch == maxPatch)

    s"ordering failed: ${versions.map(v => (v, StableVersion.calcWeight(v))).mkString(",") }" |: {
      if (maxMajorCount == 1) {
        (sortedVersion.major == maxMajor) :| s"expected: ${sortedVersion.major}, got: ${maxMajor}"
      } else {
        if (maxMinorCount == 1) {
          (sortedVersion.major == maxMajor &&
           sortedVersion.minor == maxMinor) :|
            s"expected major: ${sortedVersion.major} minor ${sortedVersion.minor}, " +
            s"got major: ${maxMajor} minor: ${maxMinor}"
        } else {
          (sortedVersion.major == maxMajor &&
           sortedVersion.minor == maxMinor &&
           sortedVersion.patch == maxPatch) :|
            s"expected major: ${sortedVersion.major} minor: ${sortedVersion.minor} patch: ${sortedVersion}, " +
            s"got major: ${maxMajor} minor: ${maxMinor} patch: ${maxPatch}"
        }
      }
    }
  }
}