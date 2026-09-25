package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;

/**
 * A {@link GitContextMock} whose {@link #clone(GitUrl, Path)} copies the content of a real on-disk repository (fixture) into the target location, instead of
 * building the synthetic {@code .git} skeleton. Use this when a test needs to clone a repository and then assert on <em>real</em> file content (e.g. settings
 * or tool configuration files), while still relying on the {@link GitContextMock} behavior for {@code fetch}/{@code pull}, stash simulation, and the other
 * {@code .git} state queries.
 */
public class FixtureGitContextMock extends GitContextMock {

  private static final String COMMIT_ID = "commit-id";

  /** The on-disk repository to copy into the cloned repository. */
  private final Path fixtureSource;

  /**
   * @param context the {@link IdeContext context}.
   * @param fixtureSource the on-disk repository to copy into the cloned repository.
   */
  public FixtureGitContextMock(IdeContext context, Path fixtureSource) {

    super(context);
    this.fixtureSource = fixtureSource;
  }

  @Override
  public void clone(GitUrl gitUrl, Path repository) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.copy(this.fixtureSource, repository, FileCopyMode.COPY_TREE_CONTENT);
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
}
