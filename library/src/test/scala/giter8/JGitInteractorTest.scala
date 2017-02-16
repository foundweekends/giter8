package giter8

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, TryValues}
import G8._

import scala.collection.JavaConverters._

class JGitInteractorTest extends FlatSpec with Matchers with BeforeAndAfter with TryValues {
  import TestFileHelpers._

  var remoteRepository: File = _
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
    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe 'success

    localRepository.branch shouldBe "master"
    localRepository.commits should contain theSameElementsAs Seq("Initial commit")
  }

  it should "retrieve branch list from remote repository" in {
    val branches = Seq("fooBranch", "barBranch")

    branches foreach { branch =>
      remoteRepository.checkout(branch, createBranch = true)
    }

    interactor.getRemoteBranches(remoteRepository.getAbsolutePath).success.value should contain theSameElementsAs branches :+ "master"
  }

  it should "checkout repository to given branch" in tempDirectory { localRepository =>
    remoteRepository.checkout("firstBranch", createBranch = true)

    remoteRepository.checkout("secondBranch", createBranch = true)
    "in new branch" >> (remoteRepository / "test.txt")
    remoteRepository.commit("Create new branch")

    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe 'success
    interactor.checkoutBranch(localRepository, "firstBranch") shouldBe 'success

    localRepository.branch shouldBe "firstBranch"
    localRepository.commits should contain theSameElementsAs Seq("Initial commit")
  }

  it should "not fail if checkout existing branch" in tempDirectory { localRepository =>
    remoteRepository.checkout("master")

    interactor.cloneRepository(remoteRepository.getAbsolutePath, localRepository) shouldBe 'success
    interactor.checkoutBranch(localRepository, "master") shouldBe 'success

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
    def commits: Seq[String] = withRepository { git =>
      git.log.call.asScala.map(c => c.getFullMessage).toSeq
    }

    def branch: String = withRepository { git =>
      git.getRepository.getBranch
    }

    def commit(message: String): Unit = withRepository { git =>
      git.commit.setAll(true).setMessage(message).call()
    }

    def checkout(name: String, createBranch: Boolean = false): Unit = withRepository { git =>
      git.checkout.setName(name).setCreateBranch(createBranch).call()
    }

    private def withRepository[A](code: JGit => A): A = {
      val git = JGit.open(repository)
      try code(git)
      finally git.close()
    }
  }

}
