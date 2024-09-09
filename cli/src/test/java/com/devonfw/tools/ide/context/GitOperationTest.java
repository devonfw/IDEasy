package com.devonfw.tools.ide.context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Test of {@link GitOperation}.
 */
public class GitOperationTest extends AbstractIdeContextTest {

  private static final String URL = "https://github.com/devonfw/IDEasy.git";
  private static final String REMOTE = "origin";
  private static final String BRANCH = "main";

  @Test
  public void testFetchSkippedIfTimestampFileUpToDate(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.FETCH;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, operation.getTimestampFilename());

    // act
    operation.executeIfNeeded(context, null, repo, REMOTE, BRANCH);

    // assert
    Mockito.verifyNoInteractions(mock);
  }

  @Test
  public void testFetchCalledIfTimestampFileNotPresent(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.FETCH;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, null);

    // act
    operation.executeIfNeeded(context, null, repo, REMOTE, BRANCH);

    // assert
    Mockito.verify(mock).fetch(repo, REMOTE, BRANCH);
  }

  @Test
  public void testFetchCalledIfTimestampFileOutdated(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.FETCH;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, operation.getTimestampFilename(), true);

    // act
    operation.executeIfNeeded(context, null, repo, REMOTE, BRANCH);

    // assert
    Mockito.verify(mock).fetch(repo, REMOTE, BRANCH);
  }

  @Test
  public void testFetchCalledIfTimestampFileUpToDateButForceMode(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.FETCH;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    context.getStartContext().setForceMode(true);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, operation.getTimestampFilename(), true);

    // act
    operation.executeIfNeeded(context, null, repo, REMOTE, BRANCH);

    // assert
    Mockito.verify(mock).fetch(repo, REMOTE, BRANCH);
  }

  @Test
  public void testFetchSkippedIfTimestampFileNotPresentButOfflineMode(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.FETCH;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    context.getStartContext().setOfflineMode(true);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, null);

    // act
    operation.executeIfNeeded(context, null, repo, REMOTE, BRANCH);

    // assert
    Mockito.verifyNoInteractions(mock);
  }

  @Test
  public void testPullOrCloneSkippedIfTimestampFileUpToDate(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.PULL_OR_CLONE;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, operation.getTimestampFilename());

    // act
    operation.executeIfNeeded(context, URL, repo, null, BRANCH);

    // assert
    Mockito.verifyNoInteractions(mock);
  }

  @Test
  public void testPullOrCloneCalledIfTimestampFileNotPresent(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.PULL_OR_CLONE;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, null);

    // act
    operation.executeIfNeeded(context, URL, repo, null, BRANCH);

    // assert
    Mockito.verify(mock).pullOrClone(URL, repo, BRANCH);
  }

  @Test
  public void testPullOrCloneCalledIfTimestampFileOutdated(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.PULL_OR_CLONE;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, operation.getTimestampFilename(), true);

    // act
    operation.executeIfNeeded(context, URL, repo, null, BRANCH);

    // assert
    Mockito.verify(mock).pullOrClone(URL, repo, BRANCH);
  }

  @Test
  public void testPullOrCloneCalledIfTimestampFileUpToDateButForceMode(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.PULL_OR_CLONE;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    context.getStartContext().setForceMode(true);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, operation.getTimestampFilename(), true);

    // act
    operation.executeIfNeeded(context, URL, repo, null, BRANCH);

    // assert
    Mockito.verify(mock).pullOrClone(URL, repo, BRANCH);
  }

  @Test
  public void testPullOrCloneSkippedIfTimestampFileNotPresentButOfflineMode(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.PULL_OR_CLONE;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    context.getStartContext().setOfflineMode(true);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = createFakeGitRepo(tempDir, null);

    // act
    operation.executeIfNeeded(context, URL, repo, null, BRANCH);

    // assert
    Mockito.verifyNoInteractions(mock);
  }

  @Test
  public void testPullOrCloneSkippedIfRepoNotInitializedAndOfflineMode(@TempDir Path tempDir) throws Exception {

    // arrange
    GitOperation operation = GitOperation.PULL_OR_CLONE;
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    context.getStartContext().setOfflineMode(true);
    GitContext mock = Mockito.mock(GitContext.class);
    context.setGitContext(mock);
    Path repo = tempDir.resolve("git-repository");
    Files.createDirectories(repo);

    // act
    operation.executeIfNeeded(context, URL, repo, null, BRANCH);

    // assert
    Mockito.verify(mock).pullOrClone(URL, repo, BRANCH);
  }

  private Path createFakeGitRepo(Path dir, String file) throws Exception {

    return createFakeGitRepo(dir, file, false);
  }

  private Path createFakeGitRepo(Path dir, String file, boolean outdated) throws Exception {

    Path repo = dir.resolve("git-repository");
    Path gitFolder = repo.resolve(GitContext.GIT_FOLDER);
    Files.createDirectories(gitFolder);
    if (file != null) {
      Path timestampFile = gitFolder.resolve(file);
      Files.createFile(timestampFile);
      if (outdated) {
        Files.setLastModifiedTime(timestampFile, FileTime.fromMillis(12345678L));
      }
    }
    return repo;
  }

}
