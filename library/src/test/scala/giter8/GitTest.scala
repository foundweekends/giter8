package giter8

import java.io.File

import giter8.Git.{NoBranchError, NoTagError}
import giter8.GitInteractor.TransportError
import giter8.GitRepository.{GitHub, Local, Remote}
import org.scalatest.{EitherValues, FlatSpec, Matchers, TryValues}
import org.scalamock.scalatest.MockFactory

import scala.util.{Failure, Success, Try}

class GitTest extends FlatSpec with Matchers with EitherValues with TryValues with MockFactory {
  trait TestFixture {
    var directoryWasCleaned           = false
    var directoryWasCopied            = false
    val interactorMock: GitInteractor = mock[GitInteractor]

    val git = new Git(interactorMock) {
      // Override because actual methods can corrupt your project directory
      override protected def cleanDir(dir: File): Unit = directoryWasCleaned = true
      override protected def copy(from: File, to: File): Try[Unit] = Try {
        directoryWasCopied = true
      }
    }
  }

  "Git" should "clone repository with given branch" in new TestFixture {
    val repository  = Remote("url")
    val branch      = Some(Branch("fooBranch"))
    val destination = new File(".")

    interactorMock.getRemoteBranches _ expects repository.url returning Success(Seq(branch.get.name))
    interactorMock.cloneRepository _ expects (repository.url, destination) returning Success(())
    interactorMock.checkoutBranch _ expects (destination, "fooBranch") returning Success(())

    git.clone(repository, branch, destination)
  }

  it should "throw an error if there is no given branch" in new TestFixture {
    val repository  = Remote("url")
    val branch      = Some(Branch("nonExisting"))
    val destination = new File(".")

    interactorMock.getRemoteBranches _ expects repository.url returning Success(Seq("someOtherBranch"))
    git.clone(repository, branch, destination).failure.exception.getClass shouldBe classOf[NoBranchError]
  }

  it should "clone repository with given tag" in new TestFixture {
    val repository  = Remote("url")
    val tag         = Some(Tag("v1.0.0"))
    val destination = new File(".")

    interactorMock.getRemoteTags _ expects repository.url returning Success(Seq(tag.get.name))
    interactorMock.cloneRepository _ expects (repository.url, destination) returning Success(())
    interactorMock.checkoutTag _ expects (destination, "v1.0.0") returning Success(())

    git.clone(repository, tag, destination)
  }

  it should "throw an error if there is no given tag" in new TestFixture {
    val repository  = Remote("url")
    val tag         = Some(Tag("nonExisting"))
    val destination = new File(".")

    interactorMock.getRemoteTags _ expects repository.url returning Success(Seq("someOtherBranch"))
    git.clone(repository, tag, destination).failure.exception.getClass shouldBe classOf[NoTagError]
  }

  it should "clone repository with default branch if no branch was given" in new TestFixture {
    val repository  = Remote("url")
    val destination = new File(".")

    interactorMock.getDefaultBranch _ expects destination returning Success("default")
    interactorMock.cloneRepository _ expects (repository.url, destination) returning Success(())
    interactorMock.checkoutBranch _ expects (destination, "default") returning Success(())

    git.clone(repository, None, destination)
  }

  it should "copy local repository directory if no branch is given" in new TestFixture {
    val repository  = Local("some/repo")
    val branch      = None
    val destination = new File(".")

    git.clone(repository, branch, destination)
    assert(directoryWasCopied, "Local repository should be copied if no branch was given")
  }

  it should "retry cloning GitHub repository with given branch if clone with public URL is failed" in new TestFixture {
    val repository  = GitHub("foo", "bar")
    val branch      = Some(Branch("someBranch"))
    val destination = new File(".")

    interactorMock.getRemoteBranches _ expects repository.publicUrl returning Failure(TransportError(""))
    interactorMock.getRemoteBranches _ expects repository.privateUrl returning Success(Seq(branch.get.name))
    interactorMock.cloneRepository _ expects (repository.privateUrl, destination) returning Success(())
    interactorMock.checkoutBranch _ expects (destination, "someBranch") returning Success(())

    git.clone(repository, branch, destination)

    assert(directoryWasCleaned, "Target directory should be cleaned")
  }

  it should "retry cloning GitHub repository with default branch if clone with public URL is failed" in new TestFixture {
    val repository  = GitHub("foo", "bar")
    val branch      = None
    val destination = new File(".")

    interactorMock.cloneRepository _ expects (repository.publicUrl, destination) returning Failure(TransportError(""))
    interactorMock.cloneRepository _ expects (repository.privateUrl, destination) returning Success(())
    interactorMock.getDefaultBranch _ expects destination returning Success("default")
    interactorMock.checkoutBranch _ expects (destination, "default") returning Success(())

    git.clone(repository, branch, destination)

    assert(directoryWasCleaned, "Target directory should be cleaned")
  }
}
