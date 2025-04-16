package com.devonfw.tools.ide.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * An {@link Enum} for specific Git operations where we add caching support.
 *
 * @see GitContextImpl
 */
public enum GitOperation {

  /** {@link GitOperation} for {@link GitContext#fetch(Path, String, String)}. */
  FETCH("fetch", "FETCH_HEAD", Duration.ofMinutes(5)) {
    @Override
    protected boolean execute(IdeContext context, GitUrl gitUrl, Path targetRepository, String remote) {

      context.getGitContext().fetch(targetRepository, remote, gitUrl.branch());
      // TODO: see JavaDoc, implementation incorrect. fetch needs to return boolean if changes have been fetched
      // and then this result must be returned - or JavaDoc needs to changed
      return true;
    }
  },

  /** {@link GitOperation} for {@link GitContext#clone(GitUrl, Path)}. */
  PULL_OR_CLONE("pull/clone", "HEAD", Duration.ofMinutes(30)) {
    @Override
    protected boolean execute(IdeContext context, GitUrl gitUrl, Path targetRepository, String remote) {

      context.getGitContext().pullOrClone(gitUrl, targetRepository);
      return true;
    }
  };

  private final String name;

  private final String timestampFilename;

  private final Duration cacheDuration;

  private GitOperation(String name, String timestampFilename, Duration cacheDuration) {

    this.name = name;
    this.timestampFilename = timestampFilename;
    this.cacheDuration = cacheDuration;
  }

  /**
   * @return the human readable name of this {@link GitOperation}.
   */
  public String getName() {

    return this.name;
  }

  /**
   * @return the name of the file inside the ".git" folder to get the timestamp (modification time) from in order to determine how long the last
   *     {@link GitOperation} was ago.
   */
  public String getTimestampFilename() {

    return this.timestampFilename;
  }

  /**
   * @return the {@link Duration} how long this {@link GitOperation} will be skipped.
   */
  public Duration getCacheDuration() {

    return cacheDuration;
  }

  /**
   * @return {@code true} if this operation requires the ".git" folder to be present, {@code false} otherwise.
   */
  public boolean isRequireGitFolder() {

    return this == FETCH;
  }

  /**
   * @return {@code true} if after this operation the {@link #getTimestampFilename() timestamp file} should be updated, {@code false} otherwise.
   */
  public boolean isForceUpdateTimestampFile() {

    return this == PULL_OR_CLONE;
  }

  /**
   * @return {@code true} if after this operation is always {@link #isNeeded(Path, IdeContext) needed} of the ".git" folder not is present, {@code false}
   *     otherwise.
   */
  public boolean isNeededIfGitFolderNotPresent() {

    return this == PULL_OR_CLONE;
  }

  /**
   * Executes this {@link GitOperation} physically.
   *
   * @param context the {@link IdeContext}.
   * @param gitUrl the git repository URL. Maybe {@code null} if not required by the operation.
   * @param targetRepository the {@link Path} to the git repository.
   * @param remote the git remote (e.g. "origin"). Maybe {@code null} if not required by the operation.
   * @return {@code true} if changes were received from git, {@code false} otherwise.
   */
  protected abstract boolean execute(IdeContext context, GitUrl gitUrl, Path targetRepository, String remote);

  /**
   * Executes this {@link GitOperation} if {@link #isNeeded(Path, IdeContext) needed}.
   *
   * @param context the {@link IdeContext}.
   * @param gitUrl the git repository URL. Maybe {@code null} if not required by the operation.
   * @param targetRepository the {@link Path} to the git repository.
   * @param remote the git remote (e.g. "origin"). Maybe {@code null} if not required by the operation.
   * @return {@code true} if changes were received from git, {@code false} otherwise (e.g. no git operation was invoked at all).
   */
  boolean executeIfNeeded(IdeContext context, GitUrl gitUrl, Path targetRepository, String remote) {

    if (isNeeded(targetRepository, context)) {
      boolean result = execute(context, gitUrl, targetRepository, remote);
      if (isForceUpdateTimestampFile()) {
        Path timestampPath = targetRepository.resolve(GitContext.GIT_FOLDER).resolve(this.timestampFilename);
        try {
          context.getFileAccess().touch(timestampPath);
        } catch (IllegalStateException e) {
          context.warning(e.getMessage());
        }
      }
      return result;
    } else {
      context.trace("Skipped git {}.", this.name);
      return false;
    }
  }

  private boolean isNeeded(Path targetRepository, IdeContext context) {

    Path gitDirectory = targetRepository.resolve(".git");
    boolean hasGitDirectory = Files.isDirectory(gitDirectory);
    if (isNeededIfGitFolderNotPresent() && !hasGitDirectory) {
      logEnforceGitOperationBecauseGitFolderNotPresent(targetRepository, context);
      return true;
    }
    if (context.isOffline()) {
      context.info("Skipping git {} on {} because we are offline.", this.name, targetRepository);
      return false;
    } else if (context.isForceMode() || context.isForcePull()) {
      context.debug("Enforcing git {} on {} because force mode is active.", this.name, targetRepository);
      return true;
    }
    if (!hasGitDirectory) {
      if (isRequireGitFolder()) {
        if (context.getSettingsGitRepository() == null) {
          context.warning("Missing .git folder in {}.", targetRepository);
        }
      } else {
        logEnforceGitOperationBecauseGitFolderNotPresent(targetRepository, context);
      }
      return true; // technically this is an error that will be triggered by fetch method
    }
    Path timestampFilePath = gitDirectory.resolve(this.timestampFilename);
    if (context.getFileAccess().isFileAgeRecent(timestampFilePath, this.cacheDuration)) {
      context.debug("Skipping git {} on {} because last fetch was just recently to avoid overhead.", this.name,
          targetRepository);
      return false;
    } else {
      context.debug("Will need to do git {} on {} because last fetch was some time ago.", this.name, targetRepository);
      return true;
    }
  }

  private void logEnforceGitOperationBecauseGitFolderNotPresent(Path targetRepository, IdeContext context) {
    context.debug("Enforcing git {} on {} because .git folder is not present.", this.name, targetRepository);
  }

}
