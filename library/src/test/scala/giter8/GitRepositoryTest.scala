package giter8

import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.util.Success

class GitRepositoryTest extends FlatSpec with Matchers with TryValues {

  "JGit" should "resolve repo name correctly" in {
    val testCases: Map[String, GitRepository] = Map(
      "git@some.path.com/repo" -> GitRepository.Remote("git@some.path.com/repo"),
      "git://some.path.com/repo" -> GitRepository.Remote("git://some.path.com/repo"),
      "https://some.path.com/repo" -> GitRepository.Remote("https://some.path.com/repo"),
      "http://some.path.com/repo" -> GitRepository.Remote("http://some.path.com/repo"),
      "ssh://some.path.com/repo" -> GitRepository.Remote("ssh://some.path.com/repo"),
      "file://relative/path" -> GitRepository.Local("relative/path"),
      "file:///home/foo/bar" -> GitRepository.Local("/home/foo/bar"),
      "foo/bar" -> GitRepository.GitHub("foo", "bar")
    )

    testCases foreach {
      case (string, repository) =>
        GitRepository.fromString(string).success.value shouldBe repository
    }
  }

}
