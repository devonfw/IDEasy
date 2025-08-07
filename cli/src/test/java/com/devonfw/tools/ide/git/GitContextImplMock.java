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

  private static final String COMMIT_ID = "commit-id";
  
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
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.copy(this.repositoryPath, repository.getParent());
    try {
      // Create .git/FETCH_HEAD and .git/HEAD files
      Path gitFolder = repository.resolve(GIT_FOLDER);
      Files.createDirectory(gitFolder);
      fileAccess.touch(gitFolder.resolve(FILE_FETCH_HEAD));
      fileAccess.writeFileContent(gitUrl.toString(), gitFolder.resolve(FILE_HEAD));
      fileAccess.writeFileContent("70b100e95a5f6c48ae70e8eea302c48ad4874bd4", gitFolder.resolve(COMMIT_ID));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String determineRemote(Path repository) {

    return DEFAULT_REMOTE;
  }

  @Override
  public String determineCurrentBranch(Path repository) {

    return GitUrl.BRANCH_MAIN;
  }

  @Override
  protected String determineCurrentCommitId(Path repository) {

    Path gitFolder = repository.resolve(GIT_FOLDER);
    return this.context.getFileAccess().readFileContent(gitFolder.resolve(FILE_HEAD));
  }

  @Override
  public void reset(Path repository, String branchName, String remoteName) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.listChildren(repository, f -> Files.isRegularFile(f) && f.getFileName().toString().contains("modified"))
        .forEach(fileAccess::delete);
  }


}
