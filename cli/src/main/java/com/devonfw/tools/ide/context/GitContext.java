package com.devonfw.tools.ide.context;

import com.devonfw.tools.ide.cli.CliOfflineException;

import java.nio.file.Path;

/**
 * Interface for git commands with input and output of information for the user.
 */
public interface GitContext {

  /** The default git remote name. */
  String DEFAULT_REMOTE = "origin";

  /** The default git url of the settings repository for IDEasy developers */
  String DEFAULT_SETTINGS_GIT_URL = "https://github.com/devonfw/ide-settings.git";

  /**
   * Checks if the Git repository in the specified target folder needs an update by inspecting the modification time of a magic file.
   *
   * @param repoUrl the git remote URL to clone from.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrCloneIfNeeded(String repoUrl, String branch, Path targetRepository);

  /**
   * Attempts a git pull and reset if required.
   *
   * @param repoUrl the git remote URL to clone from.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  default void pullOrCloneAndResetIfNeeded(String repoUrl, Path targetRepository) {

    pullOrCloneAndResetIfNeeded(repoUrl, targetRepository, null);
  }

  /**
   * Attempts a git pull and reset if required.
   *
   * @param repoUrl the git remote URL to clone from.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  default void pullOrCloneAndResetIfNeeded(String repoUrl, Path targetRepository, String branch) {

    pullOrCloneAndResetIfNeeded(repoUrl, targetRepository, branch, null);
  }

  /**
   * Attempts a git pull and reset if required.
   *
   * @param repoUrl the git remote URL to clone from.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   * @param remoteName the remote name e.g. origin.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrCloneAndResetIfNeeded(String repoUrl, Path targetRepository, String branch, String remoteName);

  /**
   * Runs a git pull or a git clone.
   *
   * @param gitRepoUrl the git remote URL to clone from.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrClone(String gitRepoUrl, Path targetRepository);

  /**
   * Runs a git pull or a git clone.
   *
   * @param gitRepoUrl the git remote URL to clone from.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrClone(String gitRepoUrl, String branch, Path targetRepository);

  /**
   * Runs a git clone.
   *
   * @param gitRepoUrl the {@link GitUrl} to use for the repository URL.
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void clone(GitUrl gitRepoUrl, Path targetRepository);

  /**
   * Runs a git pull.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   */
  void pull(Path targetRepository);

  /**
   * Runs a git diff-index to detect local changes and if so reverts them via git reset.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   */
  default void reset(Path targetRepository) {

    reset(targetRepository, null);
  }

  /**
   * Runs a git diff-index to detect local changes and if so reverts them via git reset.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   */
  default void reset(Path targetRepository, String branch) {

    reset(targetRepository, branch, null);
  }

  /**
   * Runs a git reset reverting all local changes to the git repository.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   * @param remoteName the name of the git remote e.g. "origin".
   */
  void reset(Path targetRepository, String branch, String remoteName);

  /**
   * Runs a git cleanup if untracked files were found.
   *
   * @param targetRepository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   * will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   */
  void cleanup(Path targetRepository);

  /**
   * Returns the URL of a git repository
   *
   * @param repository the {@link Path} to the folder where the git repository is located.
   * @return the url of the repository as a {@link String}.
   */
  String retrieveGitUrl(Path repository);

}
