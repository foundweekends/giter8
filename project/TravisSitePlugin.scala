import sbt._
import Keys._

import com.typesafe.sbt.sbtghpages.GhpagesPlugin
import com.typesafe.sbt.sbtghpages.GhpagesPlugin.autoImport._
import com.typesafe.sbt.SbtGit.{git, GitKeys}
import com.typesafe.sbt.git.GitRunner
import com.typesafe.sbt.site.pamflet.PamfletPlugin
import com.typesafe.sbt.site.SitePlugin

object TravisSitePlugin extends sbt.AutoPlugin {
  override def requires = PamfletPlugin && GhpagesPlugin

  import PamfletPlugin.autoImport._
  import SitePlugin.autoImport._

  object autoImport {
    lazy val pushSiteIfChanged = taskKey[Unit]("push the site if changed")
    lazy val siteGitHubRepo    = settingKey[String]("")
    lazy val siteEmail         = settingKey[String]("")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    Pamflet / sourceDirectory := { baseDirectory.value / "docs" },
    // ghpagesBranch in ghpagesUpdatedRepository := Some("gh-pages"),
    // This task is responsible for updating the master branch on some temp dir.
    // On the branch there are files that was generated in some other ways such as:
    // - CNAME file
    //
    // This task's job is to call "git rm" on files and directories that this project owns
    // and then copy over the newly generated files.
    ghpagesSynchLocal := {
      // sync the generated site
      val repo = ghpagesUpdatedRepository.value
      val s    = streams.value
      val r    = GitKeys.gitRunner.value
      gitConfig(repo, siteEmail.value, r, s.log)
      gitRemoveFiles(repo, (repo * "*.html").get.toList, r, s)
      val mappings = for {
        (file, target) <- siteMappings.value
      } yield (file, repo / target)
      IO.copy(mappings)
      repo
    },
    // https://gist.github.com/domenic/ec8b0fc8ab45f39403dd
    // 1. generate a new SSH key: `ssh-keygen -t rsa -b 4096 -C "foo@example.com"` and
    //    name it ~/.ssh/yourprojectname_deploy_rsa
    // 2. add the public key ~/.ssh/yourprojectname_deploy_rsa.pub to GitHub: https://github.com/foo/bar/settings/keys
    // 3. copy the private key ~/.ssh/yourprojectname_deploy_rsa to ./deploy_rsa
    // 4. encrypt the token: `travis encrypt-file deploy_rsa`
    // 5. remove the private key ./deploy_rsa
    // 4. rename it to deploy_rsa.enc
    pushSiteIfChanged := (Def.taskDyn {
      val repo    = (LocalRootProject / baseDirectory).value
      val r       = GitKeys.gitRunner.value
      val s       = streams.value
      val changed = gitDocsChanged(repo, r, s.log)
      if (changed) {
        ghpagesPushSite
      } else {
        Def.task {
          s.log.info("skip push site")
        }
      }
    }).value,
    git.remoteRepo := s"git@github.com:${siteGitHubRepo.value}.git"
  )

  def gitRemoveFiles(dir: File, files: List[File], git: GitRunner, s: TaskStreams): Unit = {
    if (!files.isEmpty)
      git(("rm" :: "-r" :: "-f" :: "--ignore-unmatch" :: files.map(_.getAbsolutePath)): _*)(dir, s.log)
    ()
  }

  def gitDocsChanged(dir: File, git: GitRunner, log: Logger): Boolean = {
    // git diff --shortstat HEAD^..HEAD docs
    val range = sys.env.get("TRAVIS_COMMIT_RANGE") match {
      case Some(x) => x
      case _       => "HEAD^..HEAD"
    }
    val stat = git(("diff" :: "--shortstat" :: range :: "--" :: "docs" :: Nil): _*)(dir, log)
    stat.trim.nonEmpty
  }

  def gitConfig(dir: File, email: String, git: GitRunner, log: Logger): Unit =
    sys.env.get("CI") match {
      case Some(_) =>
        git(("config" :: "user.name" :: "foundweekends-bot[bot]" :: Nil): _*)(dir, log)
        git(("config" :: "user.email" :: email :: Nil): _*)(dir, log)
      case _ => ()
    }
}
