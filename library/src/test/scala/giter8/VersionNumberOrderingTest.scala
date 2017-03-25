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

import org.scalacheck.{Gen, Prop, Properties}
import org.scalacheck.Prop.BooleanOperators

final class VersionNumberOrderingTest extends Properties("StableVersion") {

  def genVersionNumber: Gen[VersionNumber] = for {
    n       <- Gen.choose(3, 4)
    numbers <- Gen.listOfN(n, Gen.choose(0, 1000L))
  } yield VersionNumber(numbers, Seq(), Seq())

  def genEmptyVersionNumber: Gen[VersionNumber] = Gen.const(VersionNumber(Seq(), Seq(), Seq()))

  def genVersionNumberList: Gen[Seq[VersionNumber]] = for {
    n        <- Gen.choose(1, 5)
    versions <- Gen.frequency(
                  (4, Gen.listOfN(n, genVersionNumber)),
                  (1, genEmptyVersionNumber.map(Seq(_)))
                )
  } yield versions

  property("ordering") = Prop.forAll(genVersionNumberList) { (versions: Seq[VersionNumber]) =>
    val sortedVersion = versions.sorted.head

    majorVersionShouldBeMax(sortedVersion, versions) &&
    sortedVersionIsOneOfTheSuppliedVersions(sortedVersion, versions) &&
    sortingTwiceIsTheSameAsSortingOnce(sortedVersion, versions)
  }

  private def majorVersionShouldBeMax(sortedVersion: VersionNumber, versions: Seq[VersionNumber]): Prop = {
    //major version on sorted to be the max major version in all the versions
    val sortedMajor = sortedVersion._1.getOrElse(0L)
    val maxMajor    = versions.maxBy(_._1.getOrElse(0L))._1.getOrElse(0L)

    (sortedMajor == maxMajor) :| s"expected major: ${sortedMajor}, got: ${maxMajor}"
  }

  private def sortedVersionIsOneOfTheSuppliedVersions(sortedVersion: VersionNumber, versions: Seq[VersionNumber]): Prop = {
    (versions.contains(sortedVersion)) :| s"expected sorted version: ${sortedVersion} to be one of supplied: ${versions}"
  }

  private def sortingTwiceIsTheSameAsSortingOnce(sortedVersion: VersionNumber, versions: Seq[VersionNumber]): Prop = {
    val secondSortedVersion = versions.sorted.sorted.head
    (sortedVersion == secondSortedVersion) :|
      s"sorting twice returned different results. Expected version: ${sortedVersion}, but got: ${secondSortedVersion}"
  }
}