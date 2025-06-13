package com.devonfw.tools.ide.git;

import java.nio.file.Path;

import com.devonfw.tools.ide.cli.CliOfflineException;

/**
 * Interface for git commands with input and output of information for the user.
 */
public interface GitContext {

  /** The default git remote name. */
  String DEFAULT_REMOTE = "origin";

  /** The default git url of the settings repository for IDEasy developers */
  String DEFAULT_SETTINGS_GIT_URL = "https://github.com/devonfw/ide-settings.git";

  /** The name of the internal metadata folder of a git repository. */
  String GIT_FOLDER = ".git";

  /**
   * Checks if the Git repository in the specified target folder needs an update by inspecting the modification time of a magic file.
   *
   * @param gitUrl the {@link GitUrl} to clone from.
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrCloneIfNeeded(GitUrl gitUrl, Path repository);

  /**
   * Checks if a git fetch is needed and performs it if required.
   * <p>
   * This method checks the last modified time of the `FETCH_HEAD` file in the `.git` directory to determine if a fetch is needed based on a predefined
   * threshold. If updates are available in the remote repository, it logs an information message prompting the user to pull the latest changes.
   *
   * @param repository the {@link Path} to the target folder where the git repository is located. It contains the `.git` subfolder.
   * @return {@code true} if updates were detected after fetching from the remote repository, indicating that the local repository is behind the remote. *
   *     {@code false} if no updates were detected or if no fetching was performed (e.g., the cache threshold was not met or the context is offline)
   */
  boolean fetchIfNeeded(Path repository);

  /**
   * Checks if a git fetch is needed and performs it if required.
   * <p>
   * This method checks the last modified time of the `FETCH_HEAD` file in the `.git` directory to determine if a fetch is needed based on a predefined
   * threshold. If updates are available in the remote repository, it logs an information message prompting the user to pull the latest changes.
   *
   * @param repository the {@link Path} to the target folder where the git repository is located. It contains the `.git` subfolder.
   * @param remoteName the name of the remote repository, e.g., "origin".
   * @param branch the name of the branch to check for updates.
   * @return {@code true} if updates were detected after fetching from the remote repository, indicating that the local repository is behind the remote.
   *     {@code false} if no updates were detected or if no fetching was performed (e.g., the cache threshold was not met or the context is offline)
   */
  boolean fetchIfNeeded(Path repository, String remoteName, String branch);

  /**
   * Checks if there are updates available for the Git repository in the specified target folder by comparing the local commit hash with the remote commit
   * hash.
   *
   * @param repository the {@link Path} to the target folder where the git repository is located.
   * @return {@code true} if the remote repository contains commits that are not present in the local repository, indicating that updates are available.
   *     {@code false} if the local and remote repositories are in sync, or if there was an issue retrieving the commit hashes.
   */
  boolean isRepositoryUpdateAvailable(Path repository);

  /**
   * Checks if there are updates available for the Git repository in the specified target folder by comparing the local commit hash with the remote commit
   * hash.
   *
   * @param repository the {@link Path} to the target folder where the git repository is located.
   * @param trackedCommitIdPath the {@link Path} to a file containing the last tracked commit ID of this repository.
   * @return {@code true} if the remote repository contains commits that are not present in the local repository, indicating that updates are available.
   *     {@code false} if the local and remote repositories are in sync, or if there was an issue retrieving the commit hashes.
   */
  boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath);

  /**
   * Attempts a git pull and reset if required.
   *
   * @param gitUrl the {@link GitUrl} to clone from.
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @param remoteName the remote name e.g. origin.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrCloneAndResetIfNeeded(GitUrl gitUrl, Path repository, String remoteName);

  /**
   * Runs a git pull or a git clone.
   *
   * @param gitUrl the {@link GitUrl} to clone from.
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void pullOrClone(GitUrl gitUrl, Path repository);

  /**
   * Runs a git clone.
   *
   * @param gitUrl the {@link GitUrl} to use for the repository URL.
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   * @throws CliOfflineException if offline and cloning is needed.
   */
  void clone(GitUrl gitUrl, Path repository);

  /**
   * Runs a git pull.
   *
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   */
  void pull(Path repository);

  /**
   * Runs a git diff-index to detect local changes and if so reverts them via git reset.
   *
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   */
  default void reset(Path repository) {

    reset(repository, null);
  }

  /**
   * Runs a git diff-index to detect local changes and if so reverts them via git reset.
   *
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   */
  default void reset(Path repository, String branch) {

    reset(repository, branch, null);
  }

  /**
   * Runs a git fetch.
   *
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the final folder that will contain the ".git" subfolder.
   * @param remote the name of the remote repository, e.g., "origin". If {@code null} or empty, the default remote name "origin" will be used.
   * @param branch the name of the branch to check for updates.
   */
  void fetch(Path repository, String remote, String branch);

  /**
   * Runs a git reset reverting all local changes to the git repository.
   *
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   * @param branch the explicit name of the branch to checkout e.g. "main" or {@code null} to use the default branch.
   * @param remoteName the name of the git remote e.g. "origin".
   */
  void reset(Path repository, String branch, String remoteName);

  /**
   * Runs a git cleanup if untracked files were found.
   *
   * @param repository the {@link Path} to the target folder where the git repository should be cloned or pulled. It is not the parent directory where git
   *     will by default create a sub-folder by default on clone but the * final folder that will contain the ".git" subfolder.
   */
  void cleanup(Path repository);

  /**
   * Returns the URL of a git repository
   *
   * @param repository the {@link Path} to the folder where the git repository is located.
   * @return the url of the repository as a {@link String}.
   */
  String retrieveGitUrl(Path repository);

  /**
   * Checks if there is a git installation and throws an exception if there is none
   */
  void verifyGitInstalled();

  /**
   * @param repository the {@link Path} to the folder where the git repository is located.
   * @return the name of the current branch.
   */
  String determineCurrentBranch(Path repository);

  /**
   * @param repository the {@link Path} to the folder where the git repository is located.
   * @return the name of the default origin.
   */
  String determineRemote(Path repository);

  /**
   * Saves the current git commit ID of a repository to a file given as an argument.
   *
   * @param repository the path to the git repository
   * @param trackedCommitIdPath the path to the file where the commit Id will be written.
   */
  void saveCurrentCommitId(Path repository, Path trackedCommitIdPath);
}
