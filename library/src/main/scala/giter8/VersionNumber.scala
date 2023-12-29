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

//This class has been copied from the SBT 0.13 codebase:
//https://github.com/sbt/sbt/blob/0.13/ivy/src/main/scala/sbt/VersionNumber.scala
final class VersionNumber private[giter8] (val numbers: Seq[Long], val tags: Seq[String], val extras: Seq[String]) {

  def _1: Option[Long] = get(0)
  def _2: Option[Long] = get(1)
  def _3: Option[Long] = get(2)
  def _4: Option[Long] = get(3)

  def get(idx: Int): Option[Long] =
    if (size <= idx) None
    else Some(numbers(idx))

  def size: Int = numbers.size

  /** The vector of version numbers from more to less specific from this version number. */
  lazy val cascadingVersions: Vector[VersionNumber] =
    (Vector(this) ++
      (numbers.inits filter (_.length >= 2) map (VersionNumber(_, Nil, Nil)))).distinct

  private[this] val versionStr: String =
    numbers.mkString(".") +
      (tags match {
        case Seq() => ""
        case ts    => "-" + ts.mkString("-")
      }) +
      extras.mkString("")

  override def toString: String = versionStr

  override def hashCode: Int =
    numbers.hashCode * 41 * 41 +
      tags.hashCode * 41 +
      extras.hashCode

  override def equals(o: Any): Boolean =
    o match {
      case v: VersionNumber => (this.numbers == v.numbers) && (this.tags == v.tags) && (this.extras == v.extras)
      case _                => false
    }
}

object VersionNumber {

  /** @param numbers
    *   numbers delimited by a dot.
    * @param tags
    *   string prefixed by a dash.
    * @param any
    *   other strings at the end.
    */
  def apply(numbers: Seq[Long], tags: Seq[String], extras: Seq[String]): VersionNumber =
    new VersionNumber(numbers, tags, extras)

  def apply(v: String): VersionNumber =
    unapply(v) match {
      case Some((ns, ts, es)) => VersionNumber(ns, ts, es)
      case _                  => sys.error(s"Invalid version number: $v")
    }

  def unapply(v: VersionNumber): Option[(Seq[Long], Seq[String], Seq[String])] =
    Some((v.numbers, v.tags, v.extras))

  def unapply(v: String): Option[(Seq[Long], Seq[String], Seq[String])] = {
    def splitDot(s: String): Vector[Long] =
      Option(s) match {
        case Some(x) => x.split('.').toVector.filterNot(_ == "").map(_.toLong)
        case _       => Vector()
      }

    def splitDash(s: String): Vector[String] =
      Option(s) match {
        case Some(x) => x.split('-').toVector.filterNot(_ == "")
        case _       => Vector()
      }

    def splitPlus(s: String): Vector[String] =
      Option(s) match {
        case Some(x) => x.split('+').toVector.filterNot(_ == "").map("+" + _)
        case _       => Vector()
      }

    val TaggedVersion = """(\d{1,14})([\.\d{1,14}]*)((?:-\w+)*)((?:\+.+)*)""".r

    val NonSpaceString = """(\S+)""".r

    v match {
      case TaggedVersion(m, ns, ts, es) => Some((Vector(m.toLong) ++ splitDot(ns), splitDash(ts), splitPlus(es)))
      case ""                           => None
      case NonSpaceString(s)            => Some((Vector(), Vector(), Vector(s)))
      case _                            => None
    }
  }

  // Extractor for stable dependencies (without tags)
  object Stable {
    def unapply(v: String): Option[VersionNumber] = {
      VersionNumber.unapply(v).fold[Option[VersionNumber]](None) { x =>
        if (x._2.isEmpty) Option(VersionNumber(x._1, x._2, x._3)) else None
      }
    }
  }

  implicit val versionNumberOrdering: Ordering[VersionNumber] = new Ordering[VersionNumber] {
    override def compare(v1: VersionNumber, v2: VersionNumber): Int = {
      val matchedVersions = v1.numbers.zipAll(v2.numbers, 0L, 0L)

      @scala.annotation.tailrec
      def sortVersions(versions: Seq[(Long, Long)]): Int = versions match {
        case Seq((x, y), t*) =>
          if (x > y) -1 else if (y > x) 1 else sortVersions(t)
        case _ => 0
      }

      sortVersions(matchedVersions)
    }
  }
}
