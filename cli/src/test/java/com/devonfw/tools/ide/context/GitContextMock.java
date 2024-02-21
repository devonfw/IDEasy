package com.devonfw.tools.ide.context;

import java.nio.file.Path;

import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;

public class GitContextMock implements GitContext {
  @Override
  public void pullOrCloneIfNeeded(String repoUrl, Path targetRepository) {

  }

  @Override
  public void pullOrFetchAndResetIfNeeded(String repoUrl, Path targetRepository, String remoteName, String branchName) {

  }

  @Override
  public void pullOrClone(String gitRepoUrl, Path targetRepository) {

  }

  @Override
  public void clone(GitUrl gitRepoUrl, Path targetRepository) {

  }

  @Override
  public void pull(Path targetRepository) {

  }

  @Override
  public void reset(Path targetRepository, String remoteName, String branchName) {

  }

  @Override
  public void cleanup(Path targetRepository) {

  }

  @Override
  public IdeSubLogger level(IdeLogLevel level) {

    return null;
  }
}
