package com.devonfw.tools.ide.git;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Mock implementation of {@link GitContextImpl}.
 */
public class GitContextImplMock extends GitContextImpl {

  private final Path repositoryPath;

  /**
   * @param context the {@link IdeContext context}.
   * @param repository Path to the repository.
   */
  public GitContextImplMock(IdeContext context, Path repository) {
    super(context);
    this.repositoryPath = repository;
  }

  @Override
  public void pull(Path repository) {
    // Copy the mocked repository to the target repository
    getContext().getFileAccess().copy(this.repositoryPath, repository.getParent());
  }

  @Override
  public void pullOrClone(GitUrl gitUrl, Path repository) {
    pull(repository);
  }
}
