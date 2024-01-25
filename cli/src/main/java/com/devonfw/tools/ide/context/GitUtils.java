package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.process.ProcessContext;
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
   * @param processContext the {@link ProcessContext process context}.
   * @param targetRepository the target repository Path.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   */
  public GitUtils(IdeContext context, ProcessContext processContext, Path targetRepository, String remoteName,
      String branchName) {

    this.context = context;
    this.processContext = processContext;
    this.targetRepository = targetRepository;
    this.remoteName = remoteName;
    this.branchName = branchName;

  }

  /**
   * Runs a git fetch.
   *
   */
  protected void runGitFetch() {

    ProcessResult result;
    // fetch from latest remote
    result = this.processContext.addArg("fetch").addArg(this.remoteName).addArg(this.branchName).run(true);
    if (!result.isSuccessful()) {
      this.context.warning("Git fetch from {} {} for repository {} failed.", remoteName, branchName,
          this.targetRepository);
    }
  }

  /**
   * Runs a git pull.
   */
  protected void runGitPull() {

    ProcessResult result;
    // pull from remote
    result = this.processContext.addArg("--no-pager").addArg("pull").run(true);
    if (!result.isSuccessful()) {
      context.warning("Git pull from origin master failed for repository {}.", this.targetRepository);
    }
  }

  /**
   * Runs a git reset if files were modified.
   */
  protected void runGitReset() {

    ProcessResult result;
    // check for changed files
    result = this.processContext.addArg("diff-index").addArg("--quiet").addArg("HEAD").run(true);
    if (!result.isSuccessful()) {
      // reset to origin/master
      result = this.processContext.addArg("reset").addArg("--hard").addArg(remoteName + "/" + branchName).run(true);
      if (!result.isSuccessful()) {
        context.warning("Git failed to reset {} to '{}/{}'.", remoteName, branchName, this.targetRepository);
      }
    }
  }

  /**
   * Runs a git cleanup if untracked files were found.
   */
  protected void runGitCleanup() {

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
  }
}
