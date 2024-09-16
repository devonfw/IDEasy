package com.devonfw.tools.ide.context;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;

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
    this.processContext = this.context.newProcess().executable("git").withEnvVar("GIT_TERMINAL_PROMPT", "0");
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
  public boolean isRepositoryUpdateAvailable(Path targetRepository) {

    this.processContext.directory(targetRepository);
    ProcessResult result;

    // get local commit code
    result = this.processContext.addArg("rev-parse").addArg("HEAD").run(PROCESS_MODE_FOR_FETCH);
    if (!result.isSuccessful()) {
      this.context.warning("Failed to get the local commit hash.");
      return false;
    }

    String local_commit_code = result.getOut().stream().findFirst().orElse("").trim();

    // get remote commit code
    result = this.processContext.addArg("rev-parse").addArg("@{u}").run(PROCESS_MODE_FOR_FETCH);
    if (!result.isSuccessful()) {
      this.context.warning("Failed to get the remote commit hash.");
      return false;
    }
    String remote_commit_code = result.getOut().stream().findFirst().orElse("").trim();
    return !local_commit_code.equals(remote_commit_code);
  }

  @Override
  public void pullOrCloneAndResetIfNeeded(String repoUrl, Path targetRepository, String branch, String remoteName) {

    pullOrCloneIfNeeded(repoUrl, branch, targetRepository);

    reset(targetRepository, "master", remoteName);

    cleanup(targetRepository);
  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path targetRepository) {

    pullOrClone(gitRepoUrl, targetRepository, null);
  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path targetRepository, String branch) {

    Objects.requireNonNull(targetRepository);
    Objects.requireNonNull(gitRepoUrl);

    if (!gitRepoUrl.startsWith("http")) {
      throw new IllegalArgumentException("Invalid git URL '" + gitRepoUrl + "'!");
    }

    if (Files.isDirectory(targetRepository.resolve(GIT_FOLDER))) {
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

    Optional<String[]> RemoteAndBranchOptional = getLocalRemoteAndBranch(targetRepository);

    if (RemoteAndBranchOptional.isEmpty()) {
      this.context.warning("Skipping pull!");
      return;
    }

    ProcessResult result;
    // pull from remote
    result = this.processContext.addArg("--no-pager").addArg("pull").addArg("--quiet").run(PROCESS_MODE);

    if (!result.isSuccessful()) {
      String[] RemoteAndBranch = RemoteAndBranchOptional.get();
      String remote = RemoteAndBranch[0];
      String branch = RemoteAndBranch[1];
      Map<String, String> remoteAndBranchName = new HashMap<>();
      remoteAndBranchName.put("remote", remote);
      remoteAndBranchName.put("branch", branch);
      this.context.warning("Git pull for {}/{} failed for repository {}.", remoteAndBranchName.get("remote"), remoteAndBranchName.get("branch"),
          targetRepository);
      handleErrors(targetRepository, result);


    }
  }

  @Override
  public void fetch(Path targetRepository, String remote, String branch) {

    if ((remote == null) || (branch == null)) {
      Optional<String[]> remoteAndBranchOpt = getLocalRemoteAndBranch(targetRepository);
      if (remoteAndBranchOpt.isEmpty()) {
        context.warning("Could not determine the remote or branch for the git repository at {}", targetRepository);
        return; // false;
      }
      String[] remoteAndBranch = remoteAndBranchOpt.get();
      if (remote == null) {
        remote = remoteAndBranch[0];
      }
      if (branch == null) {
        branch = remoteAndBranch[1];
      }
    }

    this.processContext.directory(targetRepository);
    ProcessResult result;

    result = this.processContext.addArg("fetch").addArg(remote).addArg(branch).run(PROCESS_MODE_FOR_FETCH);

    if (!result.isSuccessful()) {
      this.context.warning("Git fetch for '{}/{} failed.'.", remote, branch);
    }
  }

  private Optional<String[]> getLocalRemoteAndBranch(Path repositoryPath) {

    this.processContext.directory(repositoryPath);
    ProcessResult result = this.processContext.addArg("rev-parse").addArg("--abbrev-ref").addArg("--symbolic-full-name").addArg("@{u}")
        .run(PROCESS_MODE_FOR_FETCH);
    if (result.isSuccessful()) {
      String upstream = result.getOut().stream().findFirst().orElse("");
      if (upstream.contains("/")) {
        return Optional.of(upstream.split("/", 2)); // Split into remote and branch
      }
    } else {
      ProcessResult branchResult = this.processContext.addArg("branch").addArg("-vv").run(ProcessMode.DEFAULT_CAPTURE);
      List<String> branches = branchResult.getOut();
      ProcessResult repoResult = this.processContext.addArg("rev-parse").addArg("--show-toplevel").run(ProcessMode.DEFAULT_CAPTURE);
      List<String> repoResultOut = repoResult.getOut();
      String repository = repoResultOut.get(0);
      repository = repository.substring(repository.lastIndexOf("/") + 1);
      for (String branch : branches) {
        branch = branch.trim();
        if (branch.startsWith("*")) {
          branch = branch.substring(2);
          String checkedOutBranch = branch.substring(0, branch.indexOf(" "));
          context.warning("You are on branch {} of repository {}, which has no upstream configured!", checkedOutBranch, repository);
        }
      }
    }
    return Optional.empty();
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


