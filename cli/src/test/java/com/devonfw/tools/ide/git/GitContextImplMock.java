package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;

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
  public void fetch(Path repository, String remote, String branch) {
    getContext().getFileAccess().touch(repository.resolve(".git/FETCH_HEAD"));
  }

  @Override
  public void pull(Path repository) {
    getContext().getFileAccess().touch(repository.resolve(".git/HEAD"));
  }

  @Override
  public void clone(GitUrl gitUrl, Path repository) {
    // Copy the mocked repository to the target repository
    FileAccess fileAccess = getContext().getFileAccess();
    fileAccess.copy(this.repositoryPath, repository.getParent());
    try {
      // Create .git/FETCH_HEAD and .git/HEAD files
      Files.createDirectory(repository.resolve(".git"));
      fileAccess.touch(repository.resolve(".git/FETCH_HEAD"));
      fileAccess.touch(repository.resolve(".git/HEAD"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
