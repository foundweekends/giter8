package giter8

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, TryValues}
import G8._
import scala.collection.JavaConverters._


class JGitInteractorTest extends FlatSpec with Matchers with BeforeAndAfter with TryValues {
  import TestFileHelpers._

  var remoteRepositoryDirectory: File = _
  var remoteRepository: JGit = _
  var remoteRepositoryUrl: String = _

  var interactor: JGitInteractor = _

  before {
    interactor = new JGitInteractor

    remoteRepositoryDirectory = new File(FileUtils.getTempDirectory, "giter8-" + System.nanoTime)
    remoteRepositoryUrl = remoteRepositoryDirectory.getAbsolutePath
    JGit.init().setDirectory(remoteRepositoryDirectory).call()

    remoteRepository = JGit.open(remoteRepositoryDirectory)
    "test" >> (remoteRepositoryDirectory / "README.md")

    remoteRepository.commit().setMessage("Initial commit").setAll(true).call()
  }

  after {
    remoteRepositoryDirectory.delete()
  }

  "JGitInteractor" should "clone the remote repository" in tempDirectory { tempdir =>
    interactor.cloneRepository(remoteRepositoryUrl, tempdir) shouldBe 'success
    val clonedRepository = JGit.open(tempdir)

    clonedRepository.getRepository.getBranch shouldBe "master"

    val commits = clonedRepository.log.all.call.asScala
    commits.map(c => c.getFullMessage) should contain theSameElementsAs Seq("Initial commit")
  }

  it should "retrieve branch list from remote repository" in {
    val branches = Seq("fooBranch", "barBranch")

    branches foreach { branch =>
      remoteRepository.branchCreate().setName(branch).call()
    }

    interactor.getRemoteBranches(remoteRepositoryUrl).success.value should contain theSameElementsAs branches :+ "master"
  }

  it should "checkout repository to given branch" in tempDirectory { tempdir =>
    remoteRepository.checkout().setName("fooBranch").setCreateBranch(true).call()

    "in new branch" >> (remoteRepositoryDirectory / "test.txt")
    remoteRepository.commit().setAll(true).setMessage("Create new branch").call()

    remoteRepository.checkout().setName("master").call()

    interactor.cloneRepository(remoteRepositoryUrl, tempdir) shouldBe 'success
    interactor.checkoutBranch(tempdir, "fooBranch") shouldBe 'success

    JGit.open(tempdir).getRepository.getBranch shouldBe "fooBranch"
  }

  it should "retrieve default branch (where HEAD is pointing)" in tempDirectory { tempdir =>
    remoteRepository.checkout().setName("fooBranch").setCreateBranch(true).call()
    "in foo branch" >> (remoteRepositoryDirectory / "test.txt")
    remoteRepository.commit().setAll(true).setMessage("Create new branch foo").call()

    remoteRepository.checkout().setName("barBranch").setCreateBranch(true).call()
    "in bar branch" >> (remoteRepositoryDirectory / "test.txt")
    remoteRepository.commit().setAll(true).setMessage("Create new branch bar").call()

    remoteRepository.checkout().setName("fooBranch").call()

    interactor.cloneRepository(remoteRepositoryUrl, tempdir)
    interactor.getDefaultBranch(tempdir).success.value shouldBe "fooBranch"
  }
}
