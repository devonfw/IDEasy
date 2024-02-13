package com.devonfw.tools.ide.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Provides utilities for git.
 */
public class GitUtils {
  private final IdeContext context;

  private ProcessContext processContext;

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

  }

  /**
   * Runs a git pull or a git clone.
   *
   * @param force boolean true enforces a git hard reset and cleanup of added files.
   * @param gitRepoUrl String of repository URL.
   */
  public void runGitPullOrClone(boolean force, String gitRepoUrl) {

    initializeProcessContext();
    if (Files.isDirectory(this.targetRepository.resolve(".git"))) {
      // checks for remotes
      ProcessResult result = this.processContext.addArg("remote").run(true, false);
      List<String> remotes = result.getOut();
      if (remotes.isEmpty()) {
        String message = this.targetRepository
            + " is a local git repository with no remote - if you did this for testing, you may continue...\n"
            + "Do you want to ignore the problem and continue anyhow?";
        this.context.askToContinue(message);
      } else {
        this.processContext.errorHandling(ProcessErrorHandling.WARNING);

        if (!this.context.isOffline()) {
          result = runGitPull();
          if (force) {
            runGitReset();
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
      runGitClone(gitRepoUrl);
      if (!branch.isEmpty()) {
        this.processContext.addArgs("checkout", branch);
        this.processContext.run();
      }
    }
  }

  /**
   * Lazily initializes the {@link ProcessContext}.
   */
  private void initializeProcessContext() {

    if (this.processContext == null) {
      this.processContext = this.context.newProcess().directory(this.targetRepository).executable("git")
          .withEnvVar("GIT_TERMINAL_PROMPT", "0");
    }
  }

  /**
   * Runs a git clone. Throws a CliException if in offline mode.
   * 
   * @param gitRepoUrl String of repository URL.
   */
  protected void runGitClone(String gitRepoUrl) {

    initializeProcessContext();
    ProcessResult result;
    if (!this.context.isOffline()) {
      this.context.getFileAccess().mkdirs(this.targetRepository);
      this.context.requireOnline("git clone of " + gitRepoUrl);
      this.processContext.addArg("clone");
      if (this.context.isQuietMode()) {
        this.processContext.addArg("-q");
      }
      this.processContext.addArgs("--recursive", gitRepoUrl, "--config", "core.autocrlf=false", ".");
      result = this.processContext.run(true, false);
      if (!result.isSuccessful()) {
        this.context.warning("Git failed to clone {} into {}.", gitRepoUrl, this.targetRepository);
      }
    } else {
      throw new CliException(
          "Could not clone " + gitRepoUrl + " to " + this.targetRepository + " because you are offline.");
    }

  }

  /**
   * Runs a git pull.
   *
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runGitPull() {

    initializeProcessContext();
    ProcessResult result;
    // pull from remote
    result = this.processContext.addArg("--no-pager").addArg("pull").run(true, false);

    if (!result.isSuccessful()) {
      context.warning("Git pull for {}/{} failed for repository {}.", this.remoteName, this.branchName,
          this.targetRepository);
    }

    return result;
  }

  /**
   * Runs a git reset if files were modified.
   */
  protected void runGitReset() {

    initializeProcessContext();
    ProcessResult result;
    // check for changed files
    result = this.processContext.addArg("diff-index").addArg("--quiet").addArg("HEAD").run(true, false);

    if (!result.isSuccessful()) {
      // reset to origin/master
      context.warning("Git has detected modified files -- attempting to reset {} to '{}/{}'.", this.targetRepository,
          remoteName, branchName);
      result = this.processContext.addArg("reset").addArg("--hard").addArg(remoteName + "/" + branchName).run(true,
          false);

      if (!result.isSuccessful()) {
        context.warning("Git failed to reset {} to '{}/{}'.", remoteName, branchName, this.targetRepository);
      }
    }

  }

  /**
   * Runs a git cleanup if untracked files were found.
   *
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runGitCleanup() {

    initializeProcessContext();
    ProcessResult result;
    // check for untracked files
    result = this.processContext.addArg("ls-files").addArg("--other").addArg("--directory").addArg("--exclude-standard")
        .run(true, false);

    if (!result.getOut().isEmpty()) {
      // delete untracked files
      result = this.processContext.addArg("clean").addArg("-df").run(true, false);

      if (!result.isSuccessful()) {
        context.warning("Git failed to clean the repository {}.", this.targetRepository);
      }
    }

    return result;
  }
}
