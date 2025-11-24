package com.devonfw.tools.ide.git;

import java.nio.file.Path;

/**
 * Mock implementation of {@link GitContext}.
 */
public class GitContextMock implements GitContext {

  private static final String MOCKED_URL_VALUE = "mocked url value";

  @Override
  public void pullOrCloneIfNeeded(GitUrl gitUrl, Path repository) {

  }

  @Override
  public void pullOrCloneAndResetIfNeeded(GitUrl gitUrl, Path repository, String remoteName) {

  }

  @Override
  public void pullOrClone(GitUrl gitUrl, Path repository) {

  }

  @Override
  public void clone(GitUrl gitUrl, Path repository) {

  }

  @Override
  public void pull(Path repository) {

  }

  @Override
  public void fetch(Path repository, String remote, String branch) {

  }

  @Override
  public void reset(Path repository, String branchName, String remoteName) {

  }

  @Override
  public void cleanup(Path repository) {

  }

  @Override
  public String retrieveGitUrl(Path repository) {

    return MOCKED_URL_VALUE;
  }

  @Override
  public Path findGitRequired() {
    return Path.of("git");
  }

  @Override
  public boolean fetchIfNeeded(Path repository, String remoteName, String branch) {

    return false;
  }

  @Override
  public boolean fetchIfNeeded(Path repository) {

    return false;
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository) {

    return false;
  }

  @Override
  public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {

    return false;
  }

  @Override
  public String determineCurrentBranch(Path repository) {

    return "main";
  }

  @Override
  public String determineRemote(Path repository) {

    return "origin";
  }


  @Override
  public void saveCurrentCommitId(Path repository, Path trackedCommitIdPath) {

  }
}
