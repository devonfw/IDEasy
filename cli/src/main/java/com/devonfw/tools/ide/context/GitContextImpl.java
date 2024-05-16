package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the {@link GitContext}.
 */
public class GitContextImpl implements GitContext {
  private static final Duration GIT_PULL_CACHE_DELAY_MILLIS = Duration.ofMinutes(30);

  private final IdeContext context;

  private final ProcessContext processContext;

  private static final ProcessMode PROCESS_MODE = ProcessMode.DEFAULT;

  /**
   * @param context the {@link IdeContext context}.
   */
  public GitContextImpl(IdeContext context) {

    this.context = context;
    this.processContext = this.context.newProcess().executable("git").withEnvVar("GIT_TERMINAL_PROMPT", "0");
  }

  @Override
  public void pullOrCloneIfNeeded(String repoUrl, String branch, Path targetRepository) {

    Path gitDirectory = targetRepository.resolve(".git");

    // Check if the .git directory exists
    if (!this.context.isForceMode() && Files.isDirectory(gitDirectory)) {
      Path magicFilePath = gitDirectory.resolve("HEAD");
      long currentTime = System.currentTimeMillis();
      // Get the modification time of the magic file
      try {
        long fileModifiedTime = Files.getLastModifiedTime(magicFilePath).toMillis();
        // Check if the file modification time is older than the delta threshold
        if ((currentTime - fileModifiedTime > GIT_PULL_CACHE_DELAY_MILLIS.toMillis())) {
          pullOrClone(repoUrl, targetRepository);
          try {
            Files.setLastModifiedTime(magicFilePath, FileTime.fromMillis(currentTime));
          } catch (IOException e) {
            this.context.warning().log(e, "Cound not update modification-time of {}", magicFilePath);
          }
          return;
        }
      } catch (IOException e) {
        this.context.error(e);
      }
    }
    // If the .git directory does not exist or in case of an error, perform git operation directly
    pullOrClone(repoUrl, branch, targetRepository);
  }

  @Override
  public void pullOrCloneAndResetIfNeeded(String repoUrl, Path targetRepository, String branch, String remoteName) {

    pullOrCloneIfNeeded(repoUrl, branch, targetRepository);

    reset(targetRepository, "master", remoteName);

    cleanup(targetRepository);
  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path targetRepository) {

    pullOrClone(gitRepoUrl, null, targetRepository);
  }

  @Override
  public void pullOrClone(String gitRepoUrl, String branch, Path targetRepository) {

    Objects.requireNonNull(targetRepository);
    Objects.requireNonNull(gitRepoUrl);

    if (!gitRepoUrl.startsWith("http")) {
      throw new IllegalArgumentException("Invalid git URL '" + gitRepoUrl + "'!");
    }

    if (Files.isDirectory(targetRepository.resolve(".git"))) {
      // checks for remotes
      this.processContext.directory(targetRepository);
      ProcessResult result = this.processContext.addArg("remote").run(ProcessMode.DEFAULT_CAPTURE);
      List<String> remotes = result.getOut();
      if (remotes.isEmpty()) {
        String message = targetRepository + " is a local git repository with no remote - if you did this for testing, you may continue...\n"
            + "Do you want to ignore the problem and continue anyhow?";
        this.context.askToContinue(message);
      } else {
        this.processContext.errorHandling(ProcessErrorHandling.WARNING);

        if (!this.context.isOffline()) {
          pull(targetRepository);
        }
      }
    } else {
      clone(new GitUrl(gitRepoUrl, branch), targetRepository);
    }
  }

  /**
   * Handles errors which occurred during git pull.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   * @param result the {@link ProcessResult} to evaluate.
   */
  private void handleErrors(Path targetRepository, ProcessResult result) {

    if (!result.isSuccessful()) {
      String message = "Failed to update git repository at " + targetRepository;
      if (this.context.isOffline()) {
        this.context.warning(message);
        this.context.interaction("Continuing as we are in offline mode - results may be outdated!");
      } else {
        this.context.error(message);
        if (this.context.isOnline()) {
          this.context.error("See above error for details. If you have local changes, please stash or revert and retry.");
        } else {
          this.context.error("It seems you are offline - please ensure Internet connectivity and retry or activate offline mode (-o or --offline).");
        }
        this.context.askToContinue("Typically you should abort and fix the problem. Do you want to continue anyways?");
      }
    }
  }

  @Override
  public void clone(GitUrl gitRepoUrl, Path targetRepository) {

    URL parsedUrl = gitRepoUrl.parseUrl();
    this.processContext.directory(targetRepository);
    ProcessResult result;
    if (!this.context.isOffline()) {
      this.context.getFileAccess().mkdirs(targetRepository);
      this.context.requireOnline("git clone of " + parsedUrl);
      this.processContext.addArg("clone");
      if (this.context.isQuietMode()) {
        this.processContext.addArg("-q");
      }
      this.processContext.addArgs("--recursive", gitRepoUrl.url(), "--config", "core.autocrlf=false", ".");
      result = this.processContext.run(PROCESS_MODE);
      if (!result.isSuccessful()) {
        this.context.warning("Git failed to clone {} into {}.", parsedUrl, targetRepository);
      }
      String branch = gitRepoUrl.branch();
      if (branch != null) {
        this.processContext.addArgs("checkout", branch);
        result = this.processContext.run(PROCESS_MODE);
        if (!result.isSuccessful()) {
          this.context.warning("Git failed to checkout to branch {}", branch);
        }
      }
    } else {
      throw new CliOfflineException("Could not clone " + parsedUrl + " to " + targetRepository + " because you are offline.");
    }
  }

  @Override
  public void pull(Path targetRepository) {

    this.processContext.directory(targetRepository);
    ProcessResult result;
    // pull from remote
    result = this.processContext.addArg("--no-pager").addArg("pull").run(PROCESS_MODE);

    if (!result.isSuccessful()) {
      Map<String, String> remoteAndBranchName = retrieveRemoteAndBranchName();
      this.context.warning("Git pull for {}/{} failed for repository {}.", remoteAndBranchName.get("remote"), remoteAndBranchName.get("branch"),
          targetRepository);
      handleErrors(targetRepository, result);
    }
  }

  private Map<String, String> retrieveRemoteAndBranchName() {

    Map<String, String> remoteAndBranchName = new HashMap<>();
    ProcessResult remoteResult = this.processContext.addArg("branch").addArg("-vv").run(PROCESS_MODE);
    List<String> remotes = remoteResult.getOut();
    if (!remotes.isEmpty()) {
      for (String remote : remotes) {
        if (remote.startsWith("*")) {
          String checkedOutBranch = remote.substring(remote.indexOf("[") + 1, remote.indexOf("]"));
          remoteAndBranchName.put("remote", checkedOutBranch.substring(0, checkedOutBranch.indexOf("/")));
          // check if current repo is behind remote and omit message
          if (checkedOutBranch.contains(":")) {
            remoteAndBranchName.put("branch", checkedOutBranch.substring(checkedOutBranch.indexOf("/") + 1, checkedOutBranch.indexOf(":")));
          } else {
            remoteAndBranchName.put("branch", checkedOutBranch.substring(checkedOutBranch.indexOf("/") + 1));
          }

        }
      }
    } else {
      return Map.ofEntries(new AbstractMap.SimpleEntry<>("remote", "unknown"), new AbstractMap.SimpleEntry<>("branch", "unknown"));
    }

    return remoteAndBranchName;
  }

  @Override
  public void reset(Path targetRepository, String branchName, String remoteName) {

    if ((remoteName == null) || remoteName.isEmpty()) {
      remoteName = DEFAULT_REMOTE;
    }
    this.processContext.directory(targetRepository);
    ProcessResult result;
    // check for changed files
    result = this.processContext.addArg("diff-index").addArg("--quiet").addArg("HEAD").run(PROCESS_MODE);

    if (!result.isSuccessful()) {
      // reset to origin/master
      this.context.warning("Git has detected modified files -- attempting to reset {} to '{}/{}'.", targetRepository, remoteName, branchName);
      result = this.processContext.addArg("reset").addArg("--hard").addArg(remoteName + "/" + branchName).run(PROCESS_MODE);

      if (!result.isSuccessful()) {
        this.context.warning("Git failed to reset {} to '{}/{}'.", remoteName, branchName, targetRepository);
        handleErrors(targetRepository, result);
      }
    }
  }

  @Override
  public void cleanup(Path targetRepository) {

    this.processContext.directory(targetRepository);
    ProcessResult result;
    // check for untracked files
    result = this.processContext.addArg("ls-files").addArg("--other").addArg("--directory").addArg("--exclude-standard").run(ProcessMode.DEFAULT_CAPTURE);

    if (!result.getOut().isEmpty()) {
      // delete untracked files
      this.context.warning("Git detected untracked files in {} and is attempting a cleanup.", targetRepository);
      result = this.processContext.addArg("clean").addArg("-df").run(PROCESS_MODE);

      if (!result.isSuccessful()) {
        this.context.warning("Git failed to clean the repository {}.", targetRepository);
      }
    }
  }

  @Override
  public String retrieveGitUrl(Path repository) {

    this.processContext.directory(repository);
    ProcessResult result;
    result = this.processContext.addArgs("-C", repository, "remote", "-v").run(ProcessMode.DEFAULT_CAPTURE);
    for (String line : result.getOut()) {
      if (line.contains("(fetch)")) {
        return line.split("\\s+")[1]; // Extract the URL from the line
      }
    }

    this.context.error("Failed to retrieve git URL for repository: {}", repository);
    return null;
  }
}


