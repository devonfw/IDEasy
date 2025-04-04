package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.variable.IdeVariables;

/**
 * Implements the {@link GitContext}.
 */
public class GitContextImpl implements GitContext {

  private final IdeContext context;

  /**
   * @param context the {@link IdeContext context}.
   */
  public GitContextImpl(IdeContext context) {

    this.context = context;
  }

  @Override
  public void pullOrCloneIfNeeded(GitUrl gitUrl, Path repository) {

    GitOperation.PULL_OR_CLONE.executeIfNeeded(this.context, gitUrl, repository, null);
  }

  @Override
  public boolean fetchIfNeeded(Path repository) {

    return fetchIfNeeded(repository, null, null);
  }

  @Override
  public boolean fetchIfNeeded(Path repository, String remote, String branch) {

    return GitOperation.FETCH.executeIfNeeded(this.context, new GitUrl("https://dummy.url/repo.git", branch), repository, remote);
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository) {

    verifyGitInstalled();
    String localCommitId = runGitCommandAndGetSingleOutput("Failed to get the local commit id.", repository, "rev-parse", "HEAD");
    String remoteCommitId = runGitCommandAndGetSingleOutput("Failed to get the remote commit id.", repository, "rev-parse", "@{u}");
    if ((localCommitId == null) || (remoteCommitId == null)) {
      return false;
    }
    return !localCommitId.equals(remoteCommitId);
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {

    verifyGitInstalled();
    String trackedCommitId;
    try {
      trackedCommitId = Files.readString(trackedCommitIdPath);
    } catch (IOException e) {
      return false;
    }

    String remoteCommitId = runGitCommandAndGetSingleOutput("Failed to get the remote commit id.", repository, "rev-parse", "@{u}");
    return !trackedCommitId.equals(remoteCommitId);
  }

  @Override
  public void pullOrCloneAndResetIfNeeded(GitUrl gitUrl, Path repository, String remoteName) {

    pullOrCloneIfNeeded(gitUrl, repository);
    reset(repository, gitUrl.branch(), remoteName);
    cleanup(repository);
  }

  @Override
  public void pullOrClone(GitUrl gitUrl, Path repository) {

    Objects.requireNonNull(repository);
    Objects.requireNonNull(gitUrl);
    if (Files.isDirectory(repository.resolve(GIT_FOLDER))) {
      // checks for remotes
      String remote = determineRemote(repository);
      if (remote == null) {
        String message = repository + " is a local git repository with no remote - if you did this for testing, you may continue...\n"
            + "Do you want to ignore the problem and continue anyhow?";
        this.context.askToContinue(message);
      } else {
        pull(repository);
      }
    } else {
      clone(gitUrl, repository);
    }
  }

  /**
   * Handles errors which occurred during git pull.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where
   *     git will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
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
  public void clone(GitUrl gitUrl, Path repository) {

    verifyGitInstalled();
    GitUrlSyntax gitUrlSyntax = IdeVariables.PREFERRED_GIT_PROTOCOL.get(getContext());
    gitUrl = gitUrlSyntax.format(gitUrl);
    if (this.context.isOfflineMode()) {
      this.context.requireOnline("git clone of " + gitUrl);
    }
    this.context.getFileAccess().mkdirs(repository);
    List<String> args = new ArrayList<>(7);
    args.add("clone");
    if (this.context.isQuietMode()) {
      args.add("-q");
    }
    args.add("--recursive");
    args.add(gitUrl.url());
    args.add("--config");
    args.add("core.autocrlf=false");
    args.add(".");
    runGitCommand(repository, args);
    String branch = gitUrl.branch();
    if (branch != null) {
      runGitCommand(repository, "switch", branch);
    }
  }

  @Override
  public void pull(Path repository) {

    verifyGitInstalled();
    if (this.context.isOffline()) {
      this.context.info("Skipping git pull on {} because offline", repository);
      return;
    }
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT, "--no-pager", "pull", "--quiet");
    if (!result.isSuccessful()) {
      String branchName = determineCurrentBranch(repository);
      this.context.warning("Git pull on branch {} failed for repository {}.", branchName, repository);
      handleErrors(repository, result);
    }
  }

  @Override
  public void fetch(Path repository, String remote, String branch) {

    verifyGitInstalled();
    if (branch == null) {
      branch = determineCurrentBranch(repository);
    }
    if (remote == null) {
      remote = determineRemote(repository);
    }

    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT_CAPTURE, "fetch", Objects.requireNonNullElse(remote, "origin"), branch);

    if (!result.isSuccessful()) {
      this.context.warning("Git fetch for '{}/{} failed.'.", remote, branch);
    }
  }

  @Override
  public String determineCurrentBranch(Path repository) {

    verifyGitInstalled();
    return runGitCommandAndGetSingleOutput("Failed to determine current branch of git repository", repository, "branch", "--show-current");
  }

  @Override
  public String determineRemote(Path repository) {

    verifyGitInstalled();
    return runGitCommandAndGetSingleOutput("Failed to determine current origin of git repository.", repository, "remote");
  }

  @Override
  public void reset(Path repository, String branchName, String remoteName) {

    verifyGitInstalled();
    if ((remoteName == null) || remoteName.isEmpty()) {
      remoteName = DEFAULT_REMOTE;
    }
    if ((branchName == null) || branchName.isEmpty()) {
      branchName = GitUrl.BRANCH_MASTER;
    }
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT, "diff-index", "--quiet", "HEAD");
    if (!result.isSuccessful()) {
      // reset to origin/master
      this.context.warning("Git has detected modified files -- attempting to reset {} to '{}/{}'.", repository, remoteName, branchName);
      result = runGitCommand(repository, ProcessMode.DEFAULT, "reset", "--hard", remoteName + "/" + branchName);
      if (!result.isSuccessful()) {
        this.context.warning("Git failed to reset {} to '{}/{}'.", remoteName, branchName, repository);
        handleErrors(repository, result);
      }
    }
  }

  @Override
  public void cleanup(Path repository) {

    verifyGitInstalled();
    // check for untracked files
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT_CAPTURE, "ls-files", "--other", "--directory", "--exclude-standard");
    if (!result.getOut().isEmpty()) {
      // delete untracked files
      this.context.warning("Git detected untracked files in {} and is attempting a cleanup.", repository);
      runGitCommand(repository, "clean", "-df");
    }
  }

  @Override
  public String retrieveGitUrl(Path repository) {

    verifyGitInstalled();
    return runGitCommandAndGetSingleOutput("Failed to retrieve git URL for repository", repository, "config", "--get", "remote.origin.url");
  }

  IdeContext getContext() {

    return this.context;
  }

  /**
   * Checks if there is a git installation and throws an exception if there is none
   */
  private void verifyGitInstalled() {

    this.context.findBashRequired();
    Path git = Path.of("git");
    Path binaryGitPath = this.context.getPath().findBinary(git);
    if (git == binaryGitPath) {
      String message = "Could not find a git installation. We highly recommend installing git since most of our actions require git to work properly!";
      throw new CliException(message);
    }
    this.context.trace("Git is installed");
  }

  private void runGitCommand(Path directory, String... args) {

    ProcessResult result = runGitCommand(directory, ProcessMode.DEFAULT, args);
    if (!result.isSuccessful()) {
      String command = result.getCommand();
      this.context.requireOnline(command);
      result.failOnError();
    }
  }

  private void runGitCommand(Path directory, List<String> args) {

    runGitCommand(directory, args.toArray(String[]::new));
  }

  private String runGitCommandAndGetSingleOutput(String warningOnError, Path directory, String... args) {

    ProcessResult result = runGitCommand(directory, ProcessMode.DEFAULT_CAPTURE, args);
    if (result.isSuccessful()) {
      List<String> out = result.getOut();
      int size = out.size();
      if (size == 1) {
        return out.get(0);
      } else if (size == 0) {
        warningOnError += " - No output received from " + result.getCommand();
      } else {
        warningOnError += " - Expected single line of output but received " + size + " lines from " + result.getCommand();
      }
    }
    this.context.warning(warningOnError);
    return null;
  }

  private ProcessResult runGitCommand(Path directory, ProcessMode mode, String... args) {

    return runGitCommand(directory, mode, ProcessErrorHandling.LOG_WARNING, args);
  }

  private ProcessResult runGitCommand(Path directory, ProcessMode mode, ProcessErrorHandling errorHandling, String... args) {

    ProcessContext processContext = this.context.newProcess().executable("git").withEnvVar("GIT_TERMINAL_PROMPT", "0").errorHandling(errorHandling)
        .directory(directory);
    processContext.addArgs(args);
    return processContext.run(mode);
  }

  @Override
  public void saveCurrentCommitId(Path repository, Path trackedCommitIdPath) {

    if ((repository == null) || (trackedCommitIdPath == null)) {
      this.context.warning("Invalid usage of saveCurrentCommitId with null value");
      return;
    }
    this.context.trace("Saving commit Id of {} into {}", repository, trackedCommitIdPath);
    String currentCommitId = runGitCommandAndGetSingleOutput("Failed to get current commit id.", repository, "rev-parse", "HEAD");
    if (currentCommitId != null) {
      try {
        Files.writeString(trackedCommitIdPath, currentCommitId);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to save commit ID", e);
      }
    }
  }
}


