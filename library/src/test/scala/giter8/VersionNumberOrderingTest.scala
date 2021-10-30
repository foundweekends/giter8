/*
 * Original implementation (C) 2010-2015 Nathan Hamblen and contributors
 * Adapted and extended in 2016 by foundweekends project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package giter8

import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import org.scalacheck.Prop.propBoolean

final class VersionNumberOrderingTest extends Properties("StableVersion") {

  private val genChar = implicitly[Arbitrary[Char]].arbitrary

  private val defaultGenStr: Gen[String] = for {
    n   <- Gen.choose(2, 5)
    str <- Gen.listOfN(n, genChar)
  } yield str.mkString

  private def genVersionNumber: Gen[VersionNumber] =
    for {
      n       <- Gen.choose(3, 4)
      numbers <- Gen.listOfN(n, Gen.choose(0, 1000L))
      t       <- Gen.choose(1, 3)
      o       <- Gen.choose(1, 3)
      tags    <- Gen.listOfN(t, defaultGenStr)
      extras  <- Gen.listOfN(o, defaultGenStr)
    } yield VersionNumber(numbers, tags, extras)

  private def genVersionNumbers: Gen[Seq[VersionNumber]] =
    for {
      n        <- Gen.choose(1, 5)
      versions <- Gen.listOfN(n, genVersionNumber)
    } yield versions

  private def genSameMajorVersionNumbers: Gen[Seq[VersionNumber]] =
    for {
      major          <- Gen.choose(0, 1000L)
      variations     <- Gen.choose(1, 5)
      versionNumbers <- fixedPrefixVersionNumbers(major)
    } yield versionNumbers

  private def genSameMajorAndMinorVersionNumbers: Gen[Seq[VersionNumber]] =
    for {
      major          <- Gen.choose(0, 1000L)
      minor          <- Gen.choose(0, 1000L)
      versionNumbers <- fixedPrefixVersionNumbers(major, minor)
    } yield versionNumbers

  private def genSameMajorAndMinorAndPatchVersionNumbers: Gen[Seq[VersionNumber]] =
    for {
      major          <- Gen.choose(0, 1000L)
      minor          <- Gen.choose(0, 1000L)
      patch          <- Gen.choose(0, 1000L)
      versionNumbers <- fixedPrefixVersionNumbers(major, minor, patch)
    } yield versionNumbers

  private def genSameMajorAndMinorAndPatchAndOtherVersionNumbers: Gen[Seq[VersionNumber]] =
    for {
      major          <- Gen.choose(0, 1000L)
      minor          <- Gen.choose(0, 1000L)
      patch          <- Gen.choose(0, 1000L)
      other          <- Gen.choose(0, 1000L)
      versionNumbers <- fixedPrefixVersionNumbers(major, minor, patch, other)
    } yield versionNumbers

  private def genEmptyVersionNumber: Gen[VersionNumber] = Gen.const(VersionNumber(Seq(), Seq(), Seq()))

  private def fixedPrefixVersionNumbers(prefix: Long*): Gen[Seq[VersionNumber]] =
    for {
      variations <- Gen.choose(1, 5)
      versionNumbers <- Gen.listOfN(
        variations,
        genVersionNumber.map { vn =>
          VersionNumber(
            numbers = prefix.toSeq ++: vn.numbers.drop(prefix.length).filterNot(_ == 0),
            tags = vn.tags,
            extras = vn.extras
          )
        }
      )
    } yield versionNumbers

  private def genVersionNumberList: Gen[Seq[VersionNumber]] =
    for {
      versions <- Gen.frequency(
        (2, genVersionNumbers),
        (2, genSameMajorVersionNumbers),
        (2, genSameMajorAndMinorVersionNumbers),
        (2, genSameMajorAndMinorAndPatchVersionNumbers),
        (2, genSameMajorAndMinorAndPatchAndOtherVersionNumbers),
        (1, genEmptyVersionNumber.map(Seq(_)))
      )
    } yield versions

  property("ordering") = Prop.forAll(genVersionNumberList) { (versions: Seq[VersionNumber]) =>
    if (versions.isEmpty) Prop(true) // support shrinking to empty.
    else {
      val sortedVersion = versions.sorted.head

      majorVersionShouldBeMax(sortedVersion, versions) &&
      sortedVersionIsOneOfTheSuppliedVersions(sortedVersion, versions) &&
      reversingAndSortingIsTheSameAsSortingOnce(sortedVersion, versions)
    }
  }

  private def majorVersionShouldBeMax(sortedVersion: VersionNumber, versions: Seq[VersionNumber]): Prop = {
    // major version on sorted to be the max major version in all the versions
    val sortedMajor = sortedVersion._1.getOrElse(0L)
    val maxMajor    = versions.maxBy(_._1.getOrElse(0L))._1.getOrElse(0L)

    (sortedMajor == maxMajor) :| s"expected major: ${sortedMajor}, got: ${maxMajor}"
  }

  private def sortedVersionIsOneOfTheSuppliedVersions(
      sortedVersion: VersionNumber,
      versions: Seq[VersionNumber]
  ): Prop = {
    (versions
      .contains(sortedVersion)) :| s"expected sorted version: ${sortedVersion} to be one of supplied: ${versions}"
  }

  private def reversingAndSortingIsTheSameAsSortingOnce(
      sortedVersion: VersionNumber,
      versions: Seq[VersionNumber]
  ): Prop = {
    val reverseSortedVersion = versions.reverse.sorted.head
    (sortedVersion.numbers == reverseSortedVersion.numbers) :|
      s"Reversing and sorting should be the same as sorting. Expected version: ${sortedVersion}, but got: ${reverseSortedVersion}"
  }
}
