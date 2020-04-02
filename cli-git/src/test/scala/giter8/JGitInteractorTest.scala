package giter8

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.scalatest.{BeforeAndAfter, TryValues}

import scala.collection.JavaConverters._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JGitInteractorTest extends AnyFlatSpec with Matchers with BeforeAndAfter with TryValues {
  import TestFileHelpers._

  implicit class RichFile(file: File) {
    def /(child: String): File = new File(file, child)
  }

  var remoteRepository: File     = _
  var interactor: JGitInteractor = _

  before {
    interactor = new JGitInteractor

    remoteRepository = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)
    JGit.init().setDirectory(remoteRepository).call()

    "test" >> (remoteRepository / "README.md")
    remoteRepository.commit("Initial commit")
  }

  after {
    remoteRepository.delete()
  }

  "JGitInteractor" should "clone the remote repository" in tempDirectory { localRepository =>
    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe Symbol("success")

    localRepository.branch shouldBe "master"
    localRepository.commits should contain theSameElementsAs Seq("Initial commit")
  }

  it should "retrieve branch list from remote repository" in {
    val branches = Seq("fooBranch", "barBranch")

    branches foreach { branch => remoteRepository.checkout(branch, createBranch = true) }

    interactor
      .getRemoteBranches(remoteRepository.getAbsolutePath)
      .success
      .value should contain theSameElementsAs branches :+ "master"
  }

  it should "retrieve tag list from remote repository" in {
    val tags = Seq("v1.0.0", "some_tag")

    tags foreach { tag =>
      "foo" >> (remoteRepository / tag)
      remoteRepository.commit(s"New tag $tag")
      remoteRepository.tag(tag)
    }

    interactor.getRemoteTags(remoteRepository.getAbsolutePath).success.value should contain theSameElementsAs tags
  }

  it should "checkout repository to given branch" in tempDirectory { localRepository =>
    remoteRepository.checkout("firstBranch", createBranch = true)

    remoteRepository.checkout("secondBranch", createBranch = true)
    "in new branch" >> (remoteRepository / "test.txt")
    remoteRepository.commit("Create new branch")

    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe Symbol("success")
    interactor.checkoutBranch(localRepository, "firstBranch") shouldBe Symbol("success")

    localRepository.branch shouldBe "firstBranch"
    localRepository.commits should contain theSameElementsAs Seq("Initial commit")
  }

  it should "checkout to given tag" in tempDirectory { localRepository =>
    remoteRepository.tag("v1.0.0")

    "after tag" >> (remoteRepository / "test.txt")
    remoteRepository.commit("Commit after tag")

    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe Symbol("success")
    interactor.checkoutTag(localRepository, "v1.0.0") shouldBe Symbol("success")

    localRepository.commits should contain theSameElementsAs Seq("Initial commit")
  }

  it should "not fail if checkout existing branch" in tempDirectory { localRepository =>
    remoteRepository.checkout("master")

    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe Symbol("success")
    interactor.checkoutBranch(localRepository, "master") shouldBe Symbol("success")

    localRepository.branch shouldBe "master"
  }

  it should "retrieve default branch (where HEAD is pointing)" in tempDirectory { localRepository =>
    remoteRepository.checkout("firstBranch", createBranch = true)
    "in foo branch" >> (remoteRepository / "test.txt")
    remoteRepository.commit("Create new branch foo")

    remoteRepository.checkout("secondBranch", createBranch = true)
    "in bar branch" >> (remoteRepository / "test.txt")
    remoteRepository.commit("Create new branch bar")

    remoteRepository.checkout("firstBranch")

    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository)
    interactor.getDefaultBranch(localRepository).success.value shouldBe "firstBranch"
  }

  implicit class RichRepository(repository: File) {
    def commits: Seq[String] = withRepository { git => git.log.call.asScala.map(c => c.getFullMessage).toSeq }

    def branch: String = withRepository { git => git.getRepository.getBranch }

    def commit(message: String): Unit = withRepository { git => git.commit.setAll(true).setMessage(message).call() }

    def checkout(name: String, createBranch: Boolean = false): Unit = withRepository { git =>
      git.checkout.setName(name).setCreateBranch(createBranch).call()
    }

    def tag(name: String): Unit = withRepository { git => git.tag.setName(name).call() }

    private def withRepository[A](code: JGit => A): A = {
      val git = JGit.open(repository)
      try code(git)
      finally git.close()
    }
  }

}
