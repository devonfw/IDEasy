package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * An {@link Enum} for specific Git operations where we add caching support.
 *
 * @see GitContextImpl
 */
public enum GitOperation {

  FETCH("fetch", "FETCH_HEAD", Duration.ofMinutes(5)) {
    @Override
    protected boolean execute(GitContextImpl gitContext, String gitRepoUrl, Path targetRepository, String remote, String branch) {

      gitContext.fetch(targetRepository, remote, branch);
      // TODO: see JavaDoc, implementation incorrect. fetch needs to return boolean if changes have been fetched
      // and then this result must be returned - or JavaDoc needs to changed
      return true;
    }
  },

  PULL_OR_CLONE("pull/clone", "HEAD", Duration.ofMinutes(30)) {
    @Override
    protected boolean execute(GitContextImpl gitContext, String gitRepoUrl, Path targetRepository, String remote, String branch) {

      gitContext.pullOrClone(gitRepoUrl, branch, targetRepository);
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
   * Executes this {@link GitOperation} physically.
   *
   * @param gitContext the {@link GitContextImpl}.
   * @param gitRepoUrl the git repository URL. Maybe {@code null} if not required by the operation.
   * @param targetRepository the {@link Path} to the git repository.
   * @param remote the git remote (e.g. "origin"). Maybe {@code null} if not required by the operation.
   * @param branch the explicit git branch (e.g. "main"). Maybe {@code null} for default branch or if not required by the operation.
   * @return {@code true} if changes were received from git, {@code false} otherwise.
   */
  protected abstract boolean execute(GitContextImpl gitContext, String gitRepoUrl, Path targetRepository, String remote, String branch);

  /**
   * Executes this {@link GitOperation} if {@link #isNeeded(Path, IdeContext) needed}.
   *
   * @param gitContext the {@link GitContextImpl}.
   * @param gitRepoUrl the git repository URL. Maybe {@code null} if not required by the operation.
   * @param targetRepository the {@link Path} to the git repository.
   * @param remote the git remote (e.g. "origin"). Maybe {@code null} if not required by the operation.
   * @param branch the explicit git branch (e.g. "main"). Maybe {@code null} for default branch or if not required by the operation.
   * @return {@code true} if changes were received from git, {@code false} otherwise (e.g. no git operation was invoked at all).
   */
  boolean executeIfNeeded(GitContextImpl gitContext, String gitRepoUrl, Path targetRepository, String remote, String branch) {

    IdeContext context = gitContext.getContext();
    if (isNeeded(targetRepository, context)) {
      boolean result = execute(gitContext, gitRepoUrl, targetRepository, remote, branch);
      gitContext.fetch(targetRepository, remote, branch);
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

    if (context.isOffline()) {
      context.info("Skipping git {} on {} because we are offline.", this.name, targetRepository);
      return false;
    } else if (context.isForceMode()) {
      context.debug("Enforcing git {} on {} because force mode is active.", this.name, targetRepository);
      return true;
    }
    Path gitDirectory = targetRepository.resolve(".git");
    if (!Files.isDirectory(gitDirectory)) {
      if (isRequireGitFolder()) {
        context.warning("Missing .git folder in {}.", targetRepository);
      } else {
        context.debug("Enforcing git {} on {} because .git folder is not present.", this.name, targetRepository);
      }
      return true; // technically this is an error that will be triggered by fetch method
    }
    Path timestampFilePath = gitDirectory.resolve(this.timestampFilename);
    if (Files.exists(timestampFilePath)) {
      long currentTime = System.currentTimeMillis();
      try {
        long fileModifiedTime = Files.getLastModifiedTime(timestampFilePath).toMillis();
        // Check if the file modification time is older than the delta threshold
        Duration lastFileUpdateDuration = Duration.ofMillis(currentTime - fileModifiedTime);
        context.debug("In git repository {} the timestamp file {} was last updated {} ago and caching duration in {}.", targetRepository,
            timestampFilename, lastFileUpdateDuration, this.cacheDuration);
        if ((lastFileUpdateDuration.toMillis() > this.cacheDuration.toMillis())) {
          context.debug("Will need to do git {} on {} because last fetch is some time ago.", this.name, targetRepository);
          return true;
        } else {
          context.debug("Skipping git {} on {} because last fetch was just recently to avoid overhead.", this.name,
              targetRepository, this.cacheDuration);
          return false;
        }
      } catch (IOException e) {
        context.warning().log(e, "Could not update modification-time of {}. Will have to do git {}.", timestampFilePath, this.name);
        return true;
      }
    } else {
      context.debug("Will need to do git {} on {} because {} is missing.", this.name, targetRepository, timestampFilename);
      return true;
    }
  }

}
