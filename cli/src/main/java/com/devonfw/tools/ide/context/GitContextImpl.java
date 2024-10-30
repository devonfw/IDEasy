package com.devonfw.tools.ide.context;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliOfflineException;
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

  private final ProcessContext processContext;

  private static final ProcessMode PROCESS_MODE = ProcessMode.DEFAULT;
  private static final ProcessMode PROCESS_MODE_FOR_FETCH = ProcessMode.DEFAULT_CAPTURE;

  /**
   * @param context the {@link IdeContext context}.
   */
  public GitContextImpl(IdeContext context) {

    this.context = context;
    this.processContext = this.context.newProcess().executable("git").withEnvVar("GIT_TERMINAL_PROMPT", "0").errorHandling(ProcessErrorHandling.LOG_WARNING);
  }

  @Override
  public void pullOrCloneIfNeeded(String repoUrl, String branch, Path targetRepository) {

    GitOperation.PULL_OR_CLONE.executeIfNeeded(this.context, repoUrl, targetRepository, null, branch);
  }

  @Override
  public boolean fetchIfNeeded(Path targetRepository) {

    return fetchIfNeeded(targetRepository, null, null);
  }

  @Override
  public boolean fetchIfNeeded(Path targetRepository, String remote, String branch) {

    return GitOperation.FETCH.executeIfNeeded(this.context, null, targetRepository, remote, branch);
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository) {

    ProcessResult result = this.processContext.directory(repository).addArg("rev-parse").addArg("HEAD").run(PROCESS_MODE_FOR_FETCH);
    if (!result.isSuccessful()) {
      this.context.warning("Failed to get the local commit hash.");
      return false;
    }
    String localCommitHash = result.getOut().stream().findFirst().orElse("").trim();
    // get remote commit code
    result = this.processContext.addArg("rev-parse").addArg("@{u}").run(PROCESS_MODE_FOR_FETCH);
    if (!result.isSuccessful()) {
      this.context.warning("Failed to get the remote commit hash.");
      return false;
    }
    String remote_commit_code = result.getOut().stream().findFirst().orElse("").trim();
    return !localCommitHash.equals(remote_commit_code);
  }

  @Override
  public void pullOrCloneAndResetIfNeeded(String repoUrl, Path repository, String branch, String remoteName) {

    pullOrCloneIfNeeded(repoUrl, branch, repository);

    reset(repository, "master", remoteName);

    cleanup(repository);
  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path repository) {

    pullOrClone(gitRepoUrl, repository, null);
  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path repository, String branch) {

    Objects.requireNonNull(repository);
    Objects.requireNonNull(gitRepoUrl);
    if (!gitRepoUrl.startsWith("http")) {
      throw new IllegalArgumentException("Invalid git URL '" + gitRepoUrl + "'!");
    }
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
      clone(new GitUrl(gitRepoUrl, branch), repository);
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
  public void clone(GitUrl gitRepoUrl, Path targetRepository) {

    GitUrlSyntax gitUrlSyntax = IdeVariables.PREFERRED_GIT_PROTOCOL.get(getContext());
    URL parsedUrl = gitRepoUrl.convert(gitUrlSyntax).parseUrl();
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
      throw CliOfflineException.ofClone(parsedUrl, targetRepository);
    }
  }

  @Override
  public void pull(Path repository) {

    if (this.context.isOffline()) {
      this.context.info("Skipping git pull on {} because offline", repository);
      return;
    }
    ProcessResult result = this.processContext.directory(repository).addArg("--no-pager").addArg("pull").addArg("--quiet").run(PROCESS_MODE);
    if (!result.isSuccessful()) {
      String branchName = determineCurrentBranch(repository);
      this.context.warning("Git pull on branch {} failed for repository {}.", branchName, repository);
      handleErrors(repository, result);
    }
  }

  @Override
  public void fetch(Path targetRepository, String remote, String branch) {

    if (branch == null) {
      branch = determineCurrentBranch(targetRepository);
    }
    if (remote == null) {
      remote = determineRemote(targetRepository);
    }
    ProcessResult result = this.processContext.directory(targetRepository).addArg("fetch").addArg(remote).addArg(branch).run(PROCESS_MODE_FOR_FETCH);

    if (!result.isSuccessful()) {
      this.context.warning("Git fetch for '{}/{} failed.'.", remote, branch);
    }
  }

  @Override
  public String determineCurrentBranch(Path repository) {

    ProcessResult remoteResult = this.processContext.directory(repository).addArg("branch").addArg("--show-current").run(ProcessMode.DEFAULT_CAPTURE);
    if (remoteResult.isSuccessful()) {
      List<String> remotes = remoteResult.getOut();
      if (!remotes.isEmpty()) {
        assert (remotes.size() == 1);
        return remotes.get(0);
      }
    } else {
      this.context.warning("Failed to determine current branch of git repository {}", repository);
    }
    return null;
  }

  @Override
  public String determineRemote(Path repository) {

    ProcessResult remoteResult = this.processContext.directory(repository).addArg("remote").run(ProcessMode.DEFAULT_CAPTURE);
    if (remoteResult.isSuccessful()) {
      List<String> remotes = remoteResult.getOut();
      if (!remotes.isEmpty()) {
        assert (remotes.size() == 1);
        return remotes.get(0);
      }
    } else {
      this.context.warning("Failed to determine current origin of git repository {}", repository);
    }
    return null;
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

  IdeContext getContext() {

    return this.context;
  }
}


