package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
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
    executeGitOperation(GitOperation.PULL_OR_CLONE, gitUrl, repository, null);
  }

  @Override
  public boolean fetchIfNeeded(Path repository) {
    return executeGitOperation(GitOperation.FETCH, new GitUrl("https://dummy.url/repo.git", null), repository, null);
  }

  @Override
  public boolean fetchIfNeeded(Path repository, String remote, String branch) {
    return executeGitOperation(GitOperation.FETCH, new GitUrl("https://dummy.url/repo.git", branch), repository, remote);
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository) {
    return compareCommitIds(repository, "HEAD", "@{u}");
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {
    String trackedCommitId = readFileContent(trackedCommitIdPath);
    String remoteCommitId = getCommitId(repository, "@{u}");
    return !Objects.equals(trackedCommitId, remoteCommitId);
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
    if (isGitRepository(repository)) {
      String remote = determineRemote(repository);
      if (remote == null) {
        this.context.askToContinue(repository + " is a local git repository with no remote. Do you want to continue?");
      } else {
        pull(repository);
      }
    } else {
      clone(gitUrl, repository);
    }
  }

  @Override
  public void clone(GitUrl gitUrl, Path repository) {

    verifyGitInstalled();
    gitUrl = IdeVariables.PREFERRED_GIT_PROTOCOL.get(context).format(gitUrl);
    requireOnline("git clone of " + gitUrl);
    this.context.getFileAccess().mkdirs(repository);
    List<String> args = List.of("clone", "--recursive", gitUrl.url(), "--config", "core.autocrlf=false", ".");
    runGitCommand(repository, args);
    switchBranch(repository, gitUrl.branch());
  }

  @Override
  public void pull(Path repository) {

    verifyGitInstalled();
    if (this.context.isOffline()) {
      this.context.info("Skipping git pull on {} because offline", repository);
      return;
    }
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT, "--no-pager", "pull", "--quiet");
    handleErrors(repository, result, "Git pull failed.");
  }

  @Override
  public void fetch(Path repository, String remote, String branch) {

    verifyGitInstalled();
    runGitCommand(repository, ProcessMode.DEFAULT_CAPTURE, "fetch", Objects.requireNonNullElse(remote, "origin"), branch);
  }

  @Override
  public String determineCurrentBranch(Path repository) {
    return getCommitId(repository, "branch", "--show-current");
  }

  @Override
  public String determineRemote(Path repository) {
    return getCommitId(repository, "remote");
  }

  @Override
  public void reset(Path repository, String branchName, String remoteName) {

    verifyGitInstalled();
    branchName = Objects.requireNonNullElse(branchName, GitUrl.BRANCH_MASTER);
    remoteName = Objects.requireNonNullElse(remoteName, DEFAULT_REMOTE);
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT, "diff-index", "--quiet", "HEAD");
    if (!result.isSuccessful()) {
      this.context.warning("Resetting {} to {}/{}.", repository, remoteName, branchName);
      runGitCommand(repository, ProcessMode.DEFAULT, "reset", "--hard", remoteName + "/" + branchName);
    }
  }

  @Override
  public void cleanup(Path repository) {

    verifyGitInstalled();
    // check for untracked files
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT_CAPTURE, "ls-files", "--other", "--directory", "--exclude-standard");
    if (!result.getOut().isEmpty()) {
      this.context.warning("Cleaning up untracked files in {}.", repository);
      runGitCommand(repository, "clean", "-df");
    }
  }

  @Override
  public String retrieveGitUrl(Path repository) {
    return getCommitId(repository, "config", "--get", "remote.origin.url");
  }

  @Override
  public void saveCurrentCommitId(Path repository, Path trackedCommitIdPath) {
    String currentCommitId = getCommitId(repository, "rev-parse", "HEAD");
    if (currentCommitId != null) {
      writeFileContent(trackedCommitIdPath, currentCommitId);
    }
  }

  private boolean executeGitOperation(GitOperation operation, GitUrl gitUrl, Path repository, String remote) {
    return operation.executeIfNeeded(this.context, gitUrl, repository, remote);
  }

  private boolean compareCommitIds(Path repository, String localRef, String remoteRef) {
    String localCommitId = getCommitId(repository, "rev-parse", localRef);
    String remoteCommitId = getCommitId(repository, "rev-parse", remoteRef);
    return !Objects.equals(localCommitId, remoteCommitId);
  }

  private String getCommitId(Path repository, String... args) {
    ProcessResult result = runGitCommand(repository, ProcessMode.DEFAULT_CAPTURE, args);
    return result.isSuccessful() && !result.getOut().isEmpty() ? result.getOut().getFirst() : null;
  }

  private void switchBranch(Path repository, String branch) {
    if (branch != null) {
      runGitCommand(repository, "switch", branch);
    }
  }

  private void handleErrors(Path repository, ProcessResult result, String errorMessage) {
    if (!result.isSuccessful()) {
      this.context.warning(errorMessage);
      if (this.context.isOnline()) {
        this.context.error("See above error for details. If you have local changes, please stash or revert and retry.");
      } else {
        this.context.error("Ensure Internet connectivity and retry or activate offline mode.");
      }
      this.context.askToContinue("Do you want to continue anyway?");
    }
  }

  private void verifyGitInstalled() {
    Path git = Path.of("git");
    if (this.context.getPath().findBinary(git) == git) {
      throw new CliException("Git installation not found. Please install git.");
    }
  }

  private void requireOnline(String action) {
    if (this.context.isOfflineMode()) {
      this.context.requireOnline(action);
    }
  }

  private String readFileContent(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      this.context.warning("Failed to read file: {}", path);
      return null;
    }
  }

  private void writeFileContent(Path path, String content) {
    try {
      Files.writeString(path, content);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file: " + path, e);
    }
  }

  private boolean isGitRepository(Path repository) {
    return Files.isDirectory(repository.resolve(GIT_FOLDER));
  }

  private ProcessResult runGitCommand(Path directory, ProcessMode mode, String... args) {
    return this.context.newProcess()
        .executable("git")
        .withEnvVar("GIT_TERMINAL_PROMPT", "0")
        .directory(directory)
        .addArgs(args)
        .run(mode);
  }

  private ProcessResult runGitCommand(Path directory, List<String> args) {
    return runGitCommand(directory, ProcessMode.DEFAULT, args.toArray(String[]::new));
  }

  private ProcessResult runGitCommand(Path directory, String... args) {
    return runGitCommand(directory, ProcessMode.DEFAULT, args);
  }
}


