package giter8

import java.io.File

import giter8.GitRepository.{GitHub, Local, Remote}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Success, Try}

class MockRenderer extends TemplateRenderer {
  var baseDirectory: File = _
  var outputDirectory: File = _
  var arguments: Seq[String] = _
  var forceOverwrite: Boolean = _

  override def render(baseDirectory: File, outputDirectory: File, arguments: Seq[String], forceOverwrite: Boolean): Either[String, String] = {
    this.baseDirectory = baseDirectory
    this.outputDirectory = outputDirectory
    this.arguments = arguments
    this.forceOverwrite = forceOverwrite
    Right("OK")
  }
}

class MockGit extends Git(null) {
  var repository: GitRepository = _
  var branch: Option[String] = _
  var dest: File = _

  override def clone(repository: GitRepository, branch: Option[String], dest: File): Try[Unit] = {
    this.repository = repository
    this.branch = branch
    Success(())
  }
}

class JgitHelperTest extends FlatSpec with Matchers {

  trait TestFixture {
    val git = new MockGit
    val renderer = new MockRenderer
    val helper = new JgitHelper(git, renderer)
  }

  "JGitHelper" should "clone repo with correct URL and branch" in {
    case class TestCase(config: Config, repository: GitRepository, branch: Option[String])
    val testCases: Seq[TestCase] = Seq(
      TestCase(
        Config("file:///foo", branch = Some("baz")),
        Local("/foo"),
        Some("baz")),
      TestCase(
        Config("https://github.com/foo/bar.g8.git", branch = None),
        Remote("https://github.com/foo/bar.g8.git"),
        None),
      TestCase(
        Config("foo/bar", branch = Some("baz")),
        GitHub("foo", "bar"),
        Some("baz"))
    )

    testCases foreach { testCase =>
      new TestFixture {
        helper.run(testCase.config, Seq.empty, new File("."))
        git.repository shouldBe testCase.repository
        git.branch shouldBe testCase.branch
      }
    }
  }

  it should "render template with correct arguments" in {
    val testCases = Seq(Seq.empty, Seq("foo", "bar"))

    testCases foreach { arguments =>
      new TestFixture {
        helper.run(Config("foo/bar", None), arguments, new File("."))
        renderer.arguments should contain theSameElementsAs arguments
      }
    }
  }

  it should "render template with correct output directory" in {
    val testCases = Seq(new File("."))

    testCases foreach { outputDir =>
      new TestFixture {
        helper.run(Config("foo/bar", None), Seq.empty, outputDir)
        renderer.outputDirectory shouldBe outputDir
      }
    }
  }

  it should "pass forceOverwrite flag to renderer" in new TestFixture {
    helper.run(Config("foo/bar", None, forceOverwrite = true), Seq.empty, new File(""))
    renderer.forceOverwrite shouldBe true
  }

  it should "pass directory to renderer" in new TestFixture {
    helper.run(Config("foo/bar", None, forceOverwrite = true, Some("directory")), Seq.empty, new File(""))
    renderer.baseDirectory.getAbsolutePath().endsWith("directory") shouldBe true
  }
}
