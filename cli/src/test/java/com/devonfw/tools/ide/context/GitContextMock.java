package com.devonfw.tools.ide.context;

import java.nio.file.Path;

/**
 * Mock implementation of {@link GitContext}.
 */
public class GitContextMock implements GitContext {
  @Override
  public void pullOrCloneIfNeeded(String repoUrl, String branch, Path targetRepository) {

  }

  @Override
  public void pullOrCloneAndResetIfNeeded(String repoUrl, Path targetRepository, String branch, String remoteName) {

  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path targetRepository) {

  }

  @Override
  public void pullOrClone(String gitRepoUrl, String branch, Path targetRepository) {

  }

  @Override
  public void clone(GitUrl gitRepoUrl, Path targetRepository) {

  }

  @Override
  public void pull(Path targetRepository) {

  }

  @Override
  public void reset(Path targetRepository, String branchName, String remoteName) {

  }

  @Override
  public void cleanup(Path targetRepository) {

  }

  @Override
  public String retrieveGitUrl(Path repository) {

    return null;
  }
}
