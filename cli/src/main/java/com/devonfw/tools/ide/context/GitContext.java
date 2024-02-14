package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogger;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Interface for git commands with input and output of information for the user.
 */
public interface GitContext extends IdeLogger {

  /**
   * Checks if the Git repository in the specified target folder needs an update by inspecting the modification time of
   * a magic file.
   *
   * @param repoUrl the git remote URL to clone from. May be suffixed with a hash-sign ('#') followed by the branch name
   *        to check-out.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   * @param force boolean true enforces a git hard reset and cleanup of added files.
   */
  void gitPullOrCloneIfNeeded(String repoUrl, Path targetRepository, String remoteName, String branchName,
      boolean force);

  /**
   * Runs a git pull or a git clone.
   *
   * @param gitRepoUrl the git remote URL to clone from. May be suffixed with a hash-sign ('#') followed by the branch
   *        name to check-out.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   * @param force boolean true enforces a git hard reset and cleanup of added files.
   */
  void runGitPullOrClone(String gitRepoUrl, Path targetRepository, String remoteName, String branchName, boolean force);

  /**
   * Lazily initializes the {@link ProcessContext}.
   * 
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   */
  void initializeProcessContext(Path targetRepository);

  /**
   * Runs a git clone. Throws a CliException if in offline mode.
   *
   * @param gitRepoUrl String of repository URL.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   */
  void runGitClone(String gitRepoUrl, Path targetRepository);

  /**
   * Runs a git pull.
   * 
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   * @return the {@link ProcessResult}.
   */
  ProcessResult runGitPull(Path targetRepository, String remoteName, String branchName);

  /**
   * Runs a git reset if files were modified.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   */
  void runGitReset(Path targetRepository, String remoteName, String branchName);

  /**
   * Runs a git cleanup if untracked files were found.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   *        * It is not the parent directory where git will by default create a sub-folder by default on clone but the *
   *        final folder that will contain the ".git" subfolder.
   * 
   * @return the {@link ProcessResult}.
   */
  ProcessResult runGitCleanup(Path targetRepository);

}
