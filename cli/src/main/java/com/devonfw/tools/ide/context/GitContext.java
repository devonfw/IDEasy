package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Interface for git commands with input and output of information for the user.
 */
public interface GitContext extends IdeLogger {

  /**
   * Checks if the Git repository in the specified target folder needs an update by inspecting the modification time of
   * a magic file.
   *
   * @param repoUrl the git remote URL to clone from. May be suffixed with a hash-sign ('#') followed by the branch name
   * to check-out.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   */
  void pullOrCloneIfNeeded(String repoUrl, String branch, Path targetRepository);

  /**
   * Attempts a git pull and reset if required.
   *
   * @param repoUrl the git remote URL to clone from. May be suffixed with a hash-sign ('#') followed by the branch name
   * to check-out.
   * @param branch the branch name e.g. master.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   * @param remoteName the remote name e.g. origin.
   */
  void pullOrFetchAndResetIfNeeded(String repoUrl, String branch, Path targetRepository, String remoteName);

  /**
   * Runs a git pull or a git clone.
   *
   * @param gitRepoUrl the git remote URL to clone from. May be suffixed with a hash-sign ('#') followed by the branch
   * name to check-out.
   * @param branch the branch name e.g. master.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   */
  void pullOrClone(String gitRepoUrl, String branch, Path targetRepository);

  /**
   * Runs a git clone. Throws a CliException if in offline mode.
   *
   * @param gitRepoUrl the {@link GitUrl} to use for the repository URL.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   */
  void clone(GitUrl gitRepoUrl, Path targetRepository);

  /**
   * Runs a git pull.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   */
  void pull(Path targetRepository);

  /**
   * Runs a git reset if files were modified.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   * @param remoteName the remote server name.
   * @param branchName the name of the branch.
   */
  void reset(Path targetRepository, String remoteName, String branchName);

  /**
   * Runs a git cleanup if untracked files were found.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled.
   * It is not the parent directory where git will by default create a sub-folder by default on clone but the * final
   * folder that will contain the ".git" subfolder.
   */
  void cleanup(Path targetRepository);

}
