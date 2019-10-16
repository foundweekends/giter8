package giter8

import java.io.File

import giter8.GitRepository.{GitHub, Local, Remote}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Success, Try}

class MockRenderer extends TemplateRenderer {
  var baseDirectory: File           = _
  var workingDirectory: File        = _
  var arguments: Seq[String]        = _
  var forceOverwrite: Boolean       = _
  var outputDirectory: Option[File] = _

  override def render(
      baseDirectory: File,
      workingDirectory: File,
      arguments: Seq[String],
      forceOverwrite: Boolean,
      outputDirectory: Option[File]
  ): Either[String, String] = {
    this.baseDirectory    = baseDirectory
    this.workingDirectory = workingDirectory
    this.arguments        = arguments
    this.forceOverwrite   = forceOverwrite
    this.outputDirectory  = outputDirectory
    Right("OK")
  }
}

class MockGit extends Git(null) {
  var repository: GitRepository = _
  var ref: Option[Ref]          = _
  var dest: File                = _

  override def clone(repository: GitRepository, ref: Option[Ref], dest: File): Try[Unit] = {
    this.repository = repository
    this.ref        = ref
    Success(())
  }
}

class JGitHelperTest extends FlatSpec with Matchers {

  trait TestFixture {
    val git      = new MockGit
    val renderer = new MockRenderer
    val helper   = new JgitHelper(git, renderer)
  }

  "JGitHelper" should "clone repo with correct URL and branch" in {
    case class TestCase(config: Config, repository: GitRepository, ref: Option[Ref])
    val testCases: Seq[TestCase] = Seq(
      TestCase(Config("file:///foo", ref = Some(Branch("baz"))), Local("/foo"), Some(Branch("baz"))),
      TestCase(
        Config("https://github.com/foo/bar.g8.git", ref = None),
        Remote("https://github.com/foo/bar.g8.git"),
        None
      ),
      TestCase(Config("foo/bar", ref = Some(Tag("baz"))), GitHub("foo", "bar"), Some(Tag("baz")))
    )

    testCases foreach { testCase =>
      new TestFixture {
        helper.run(testCase.config, Seq.empty, new File("."))
        git.repository shouldBe testCase.repository
        git.ref shouldBe testCase.ref
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
        renderer.workingDirectory shouldBe outputDir
      }
    }
  }

  it should "pass forceOverwrite flag to renderer" in new TestFixture {
    helper.run(Config("foo/bar", None, forceOverwrite = true), Seq.empty, new File(""))
    renderer.forceOverwrite shouldBe true
  }

  it should "pass directory to renderer" in new TestFixture {
    helper.run(
      Config("foo/bar", None, forceOverwrite = true, directory = Some("directory/template")),
      Seq.empty,
      new File("")
    )
    renderer.baseDirectory.getAbsolutePath().endsWith("directory/template") shouldBe true
  }
}
