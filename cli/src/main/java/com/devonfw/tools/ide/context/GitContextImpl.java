package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;

public class GitContextImpl implements GitContext {
  private static final Duration GIT_PULL_CACHE_DELAY_MILLIS = Duration.ofMillis(30 * 60 * 1000);;

  private final IdeContext context;

  private ProcessContext processContext;

  /**
   * @param context the {@link IdeContext context}.
   */
  public GitContextImpl(IdeContext context) {

    this.context = context;

  }

  @Override
  public void gitPullOrCloneIfNeeded(String repoUrl, Path targetRepository, String remoteName, String branchName,
      boolean force) {

    Path gitDirectory = targetRepository.resolve(".git");

    // Check if the .git directory exists
    if (Files.isDirectory(gitDirectory)) {
      Path magicFilePath = gitDirectory.resolve("HEAD");
      long currentTime = System.currentTimeMillis();
      // Get the modification time of the magic file
      long fileMTime;
      try {
        fileMTime = Files.getLastModifiedTime(magicFilePath).toMillis();
      } catch (IOException e) {
        throw new IllegalStateException("Could not read " + magicFilePath, e);
      }

      // Check if the file modification time is older than the delta threshold
      if ((currentTime - fileMTime > GIT_PULL_CACHE_DELAY_MILLIS.toMillis()) || force) {
        runGitPullOrClone(repoUrl, targetRepository, remoteName, branchName, force);
        try {
          Files.setLastModifiedTime(magicFilePath, FileTime.fromMillis(currentTime));
        } catch (IOException e) {
          throw new IllegalStateException("Could not read or write in " + magicFilePath, e);
        }
      }
    } else {
      // If the .git directory does not exist, perform git clone
      runGitPullOrClone(repoUrl, targetRepository, remoteName, branchName, force);
    }
  }

  @Override
  public void runGitPullOrClone(String gitRepoUrl, Path targetRepository, String remoteName, String branchName,
      boolean force) {

    Objects.requireNonNull(targetRepository);
    Objects.requireNonNull(gitRepoUrl);

    if (!gitRepoUrl.startsWith("http")) {
      throw new IllegalArgumentException("Invalid git URL '" + gitRepoUrl + "'!");
    }

    initializeProcessContext(targetRepository);
    if (Files.isDirectory(targetRepository.resolve(".git"))) {
      // checks for remotes
      ProcessResult result = this.processContext.addArg("remote").run(true, false);
      List<String> remotes = result.getOut();
      if (remotes.isEmpty()) {
        String message = targetRepository
            + " is a local git repository with no remote - if you did this for testing, you may continue...\n"
            + "Do you want to ignore the problem and continue anyhow?";
        this.context.askToContinue(message);
      } else {
        this.processContext.errorHandling(ProcessErrorHandling.WARNING);

        if (!this.context.isOffline()) {
          result = runGitPull(targetRepository, remoteName, branchName);
          if (force) {
            runGitReset(targetRepository, remoteName, branchName);
            result = runGitCleanup(targetRepository);
          }
        }

        if (!result.isSuccessful()) {
          String message = "Failed to update git repository at " + targetRepository;
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
      runGitClone(gitRepoUrl, targetRepository);
      if (!branch.isEmpty()) {
        this.processContext.addArgs("checkout", branch);
        this.processContext.run();
      }
    }
  }

  @Override
  public void initializeProcessContext(Path targetRepository) {

    if (this.processContext == null) {
      this.processContext = this.context.newProcess().directory(targetRepository).executable("git")
          .withEnvVar("GIT_TERMINAL_PROMPT", "0");
    }
  }

  @Override
  public void runGitClone(String gitRepoUrl, Path targetRepository) {

    initializeProcessContext(targetRepository);
    ProcessResult result;
    if (!this.context.isOffline()) {
      this.context.getFileAccess().mkdirs(targetRepository);
      this.context.requireOnline("git clone of " + gitRepoUrl);
      this.processContext.addArg("clone");
      if (this.context.isQuietMode()) {
        this.processContext.addArg("-q");
      }
      this.processContext.addArgs("--recursive", gitRepoUrl, "--config", "core.autocrlf=false", ".");
      result = this.processContext.run(true, false);
      if (!result.isSuccessful()) {
        this.context.warning("Git failed to clone {} into {}.", gitRepoUrl, targetRepository);
      }
    } else {
      throw new CliException("Could not clone " + gitRepoUrl + " to " + targetRepository + " because you are offline.");
    }
  }

  @Override
  public ProcessResult runGitPull(Path targetRepository, String remoteName, String branchName) {

    initializeProcessContext(targetRepository);
    ProcessResult result;
    // pull from remote
    result = this.processContext.addArg("--no-pager").addArg("pull").run(true, false);

    if (!result.isSuccessful()) {
      context.warning("Git pull for {}/{} failed for repository {}.", remoteName, branchName, targetRepository);
    }

    return result;
  }

  @Override
  public void runGitReset(Path targetRepository, String remoteName, String branchName) {

    initializeProcessContext(targetRepository);
    ProcessResult result;
    // check for changed files
    result = this.processContext.addArg("diff-index").addArg("--quiet").addArg("HEAD").run(true, false);

    if (!result.isSuccessful()) {
      // reset to origin/master
      context.warning("Git has detected modified files -- attempting to reset {} to '{}/{}'.", targetRepository,
          remoteName, branchName);
      result = this.processContext.addArg("reset").addArg("--hard").addArg(remoteName + "/" + branchName).run(true,
          false);

      if (!result.isSuccessful()) {
        context.warning("Git failed to reset {} to '{}/{}'.", remoteName, branchName, targetRepository);
      }
    }
  }

  @Override
  public ProcessResult runGitCleanup(Path targetRepository) {

    initializeProcessContext(targetRepository);
    ProcessResult result;
    // check for untracked files
    result = this.processContext.addArg("ls-files").addArg("--other").addArg("--directory").addArg("--exclude-standard")
        .run(true, false);

    if (!result.getOut().isEmpty()) {
      // delete untracked files
      result = this.processContext.addArg("clean").addArg("-df").run(true, false);

      if (!result.isSuccessful()) {
        context.warning("Git failed to clean the repository {}.", targetRepository);
      }
    }

    return result;
  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    return null;
  }
}
