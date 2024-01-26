package com.devonfw.tools.ide.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Provides utilities for git
 */
public class GitUtils {
  private final IdeContext context;

  private final ProcessContext processContext;

  private final Path targetRepository;

  private final String remoteName;

  private final String branchName;

  /**
   * @param context the {@link IdeContext context}.
   * @param targetRepository the target repository Path.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   */
  public GitUtils(IdeContext context, Path targetRepository, String remoteName, String branchName) {

    this.context = context;
    this.targetRepository = targetRepository;
    this.remoteName = remoteName;
    this.branchName = branchName;
    this.processContext = this.context.newProcess().directory(targetRepository).executable("git")
        .withEnvVar("GIT_TERMINAL_PROMPT", "0");
    this.processContext.errorHandling(ProcessErrorHandling.WARNING);
  }

  /**
   * Runs a git pull or a git clone
   *
   * @param force boolean true enforces a git hard reset and cleanup of added files
   * @param gitRepoUrl String of repository URL
   */
  protected void runGitPullOrClone(boolean force, String gitRepoUrl) {

    ProcessResult result = this.processContext.addArg("remote").run(true);
    if (Files.isDirectory(this.targetRepository.resolve(".git"))) {
      List<String> remotes = result.getOut();
      if (remotes.isEmpty()) {
        String message = this.targetRepository
            + " is a local git repository with no remote - if you did this for testing, you may continue...\n"
            + "Do you want to ignore the problem and continue anyhow?";
        this.context.askToContinue(message);
      } else {
        this.processContext.errorHandling(ProcessErrorHandling.WARNING);

        if (this.context.isOnline()) {
          result = runGitFetch();
          result = runGitPull();
          if (force) {
            result = runGitReset();
            result = runGitCleanup();
          }
        }
        if (!result.isSuccessful()) {
          String message = "Failed to update git repository at " + this.targetRepository;
          if (this.context.isOffline()) {
            this.context.warning(message);
            this.context.interaction("Continuing as we are in offline mode - results may be outdated!");
          } else {
            this.context.error(message);
            if (this.context.isOnline()) {
              this.context
                  .error("See above error for details. If you have local changes, please stash or revert and retry.");
            } else {
              this.context.error(
                  "It seems you are offline - please ensure Internet connectivity and retry or activate offline mode (-o or --offline).");
            }
            this.context
                .askToContinue("Typically you should abort and fix the problem. Do you want to continue anyways?");
          }
        }
      }
    } else {
      String branch = "";
      int hashIndex = gitRepoUrl.indexOf("#");
      if (hashIndex != -1) {
        branch = gitRepoUrl.substring(hashIndex + 1);
        gitRepoUrl = gitRepoUrl.substring(0, hashIndex);
      }
      this.context.getFileAccess().mkdirs(this.targetRepository);
      this.context.requireOnline("git clone of " + gitRepoUrl);
      this.processContext.addArg("clone");
      if (this.context.isQuietMode()) {
        this.processContext.addArg("-q");
      }
      this.processContext.addArgs("--recursive", gitRepoUrl, "--config", "core.autocrlf=false", ".");
      this.processContext.run();
      if (!branch.isEmpty()) {
        this.processContext.addArgs("checkout", branch);
        this.processContext.run();
      }
    }
  }

  protected List<String> runGitGetRemotes() {

    ProcessResult result = this.processContext.addArg("remote").run(true);
    return result.getOut();
  }

  /**
   * Runs a git fetch.
   *
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runGitFetch() {

    ProcessResult result;
    // fetch from latest remote
    result = this.processContext.addArg("fetch").addArg(this.remoteName).addArg(this.branchName).run(true);

    if (!result.isSuccessful()) {
      this.context.warning("Git fetch from {} {} for repository {} failed.", remoteName, branchName,
          this.targetRepository);
    }

    return result;
  }

  /**
   * Runs a git pull.
   *
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runGitPull() {

    ProcessResult result;
    // pull from remote
    result = this.processContext.addArg("--no-pager").addArg("pull").run(true);

    if (!result.isSuccessful()) {
      context.warning("Git pull from origin master failed for repository {}.", this.targetRepository);
    }

    return result;
  }

  /**
   * Runs a git reset if files were modified.
   *
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runGitReset() {

    ProcessResult result;
    // check for changed files
    result = this.processContext.addArg("diff-index").addArg("--quiet").addArg("HEAD").run(true);

    if (!result.isSuccessful()) {
      // reset to origin/master
      context.warning("Git has detected modified files -- attempting to reset {} to '{}/{}'.", this.targetRepository,
          remoteName, branchName);
      result = this.processContext.addArg("reset").addArg("--hard").addArg(remoteName + "/" + branchName).run(true);

      if (!result.isSuccessful()) {
        context.warning("Git failed to reset {} to '{}/{}'.", remoteName, branchName, this.targetRepository);
      }
    }

    return result;
  }

  /**
   * Runs a git cleanup if untracked files were found.
   *
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runGitCleanup() {

    ProcessResult result;
    // check for untracked files
    result = this.processContext.addArg("ls-files").addArg("--other").addArg("--directory").addArg("--exclude-standard")
        .run(true);

    if (!result.getOut().isEmpty()) {
      // delete untracked files
      result = this.processContext.addArg("clean").addArg("-df").run(true);

      if (!result.isSuccessful()) {
        context.warning("Git failed to clean the repository {}.", this.targetRepository);
      }
    }

    return result;
  }
}
