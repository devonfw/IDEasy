package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link GitContextMock}.
 */
class GitContextMockTest extends AbstractIdeContextTest {

  private static final String TEST_URL = "https://github.com/test/repo.git";

  private static final String TEST_BRANCH = "develop";

  private static final String MAIN_BRANCH = "main";

  private GitContextMock newGitContextMock(Path tempDir) {

    IdeTestContext context = newContext(tempDir);
    context.getNetworkStatus().simulateOnline();

    GitContextMock mock = new GitContextMock(context);
    context.setGitContext(mock);

    return mock;
  }

  @Test
  void testCloneCreatesGitStructureWithMainBranch(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    GitUrl gitUrl = new GitUrl(TEST_URL, null);
    Path repository = tempDir.resolve("repo");

    mock.clone(gitUrl, repository);

    Path gitFolder = repository.resolve(GitContext.GIT_FOLDER);
    assertThat(gitFolder).exists().isDirectory();

    Path head = gitFolder.resolve(GitContext.FILE_HEAD);
    assertThat(head).exists().hasContent("ref: refs/heads/" + MAIN_BRANCH);

    Path fetchHead = gitFolder.resolve(GitContext.FILE_FETCH_HEAD);
    assertThat(fetchHead).exists();
    assertThat(Files.readString(fetchHead)).isNotEmpty();

    Path refFile = gitFolder.resolve("refs").resolve("heads").resolve(MAIN_BRANCH);
    assertThat(refFile).exists();
    assertThat(Files.readString(refFile)).isNotEmpty();

    Path config = gitFolder.resolve("config");
    assertThat(config).exists();
    assertThat(mock.retrieveGitUrl(repository)).isEqualTo(TEST_URL);
  }

  @Test
  void testCloneCreatesGitStructureWithCustomBranch(@TempDir Path tempDir) {

    GitContextMock mock = newGitContextMock(tempDir);
    GitUrl gitUrl = new GitUrl(TEST_URL, TEST_BRANCH);
    Path repository = tempDir.resolve("repo");

    mock.clone(gitUrl, repository);

    Path gitFolder = repository.resolve(GitContext.GIT_FOLDER);
    Path head = gitFolder.resolve(GitContext.FILE_HEAD);
    assertThat(head).hasContent("ref: refs/heads/" + TEST_BRANCH);

    Path refFile = gitFolder.resolve("refs").resolve("heads").resolve(TEST_BRANCH);
    assertThat(refFile).exists();
  }

  @Test
  void testFetchIfNeededSkipsRecentFetchHeadWithoutPendingCommits(@TempDir Path tempDir) {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    boolean result = mock.fetchIfNeeded(repository);

    assertThat(result).isFalse();
  }

  @Test
  void testFetchIfNeededSkipsRecentFetchHeadWithPendingCommits(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path contentFile = tempDir.resolve("content.txt");
    Files.writeString(contentFile, "test content");

    GitContextMock.GitChange change = new GitContextMock.GitChange(contentFile, Path.of("file.txt"));
    GitContextMock.GitCommit commit = new GitContextMock.GitCommit(change);
    mock.addChanges(repository, commit);

    boolean result = mock.fetchIfNeeded(repository);

    assertThat(result).isFalse();
  }

  @Test
  void testFetchUpdatesFetchHeadWithPendingCommits(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path fetchHeadPath = repository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_FETCH_HEAD);
    String initialFetchHead = Files.readString(fetchHeadPath);

    Path contentFile = tempDir.resolve("content.txt");
    Files.writeString(contentFile, "test content");

    GitContextMock.GitChange change = new GitContextMock.GitChange(contentFile, Path.of("file.txt"));
    GitContextMock.GitCommit commit = new GitContextMock.GitCommit(change);
    mock.addChanges(repository, commit);

    mock.fetch(repository, GitContext.DEFAULT_REMOTE, MAIN_BRANCH);

    String updatedFetchHead = Files.readString(fetchHeadPath);
    assertThat(updatedFetchHead).isNotEqualTo(initialFetchHead).isEqualTo(String.valueOf(commit.hashCode()));
  }

  @Test
  void testFetchMakesUpdateAvailable(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    assertThat(mock.isRepositoryUpdateAvailable(repository)).isFalse();

    Path contentFile = tempDir.resolve("content.txt");
    Files.writeString(contentFile, "test content");

    GitContextMock.GitChange change = new GitContextMock.GitChange(contentFile, Path.of("file.txt"));
    GitContextMock.GitCommit commit = new GitContextMock.GitCommit(change);
    mock.addChanges(repository, commit);

    mock.fetch(repository, GitContext.DEFAULT_REMOTE, MAIN_BRANCH);

    assertThat(mock.isRepositoryUpdateAvailable(repository)).isTrue();
  }

  @Test
  void testPullAppliesPendingCommitAndUpdatesRepositoryState(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path sourceFile = tempDir.resolve("source.txt");
    Files.writeString(sourceFile, "updated content");

    GitContextMock.GitChange change = new GitContextMock.GitChange(sourceFile, Path.of("tracked.txt"));
    GitContextMock.GitCommit commit = new GitContextMock.GitCommit(change);
    mock.addChanges(repository, commit);

    mock.fetch(repository, GitContext.DEFAULT_REMOTE, MAIN_BRANCH);
    assertThat(mock.isRepositoryUpdateAvailable(repository)).isTrue();

    mock.pull(repository);

    String commitHash = String.valueOf(commit.hashCode());

    assertThat(repository.resolve("tracked.txt")).exists().hasContent("updated content");

    Path refFile = repository.resolve(GitContext.GIT_FOLDER).resolve("refs").resolve("heads").resolve(MAIN_BRANCH);
    assertThat(refFile).hasContent(commitHash);

    Path fetchHeadFile = repository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_FETCH_HEAD);
    assertThat(fetchHeadFile).hasContent(commitHash);

    assertThat(mock.isRepositoryUpdateAvailable(repository)).isFalse();
    assertThat(mock.fetchIfNeeded(repository)).isFalse();
  }

  @Test
  void testPullAppliesMultipleCommitsInOrder(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path sourceFile1 = tempDir.resolve("source1.txt");
    Files.writeString(sourceFile1, "content 1");
    GitContextMock.GitCommit commit1 = new GitContextMock.GitCommit(
        new GitContextMock.GitChange(sourceFile1, Path.of("file1.txt")));

    Path sourceFile2 = tempDir.resolve("source2.txt");
    Files.writeString(sourceFile2, "content 2");
    GitContextMock.GitCommit commit2 = new GitContextMock.GitCommit(
        new GitContextMock.GitChange(sourceFile2, Path.of("file2.txt")));

    mock.addChanges(repository, commit1, commit2);

    mock.pull(repository);

    assertThat(repository.resolve("file1.txt")).exists().hasContent("content 1");
    assertThat(repository.resolve("file2.txt")).exists().hasContent("content 2");
  }

  @Test
  void testPullAppliesDirectoryChange(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path sourceDir = tempDir.resolve("sourceDir");
    Files.createDirectories(sourceDir);
    Files.writeString(sourceDir.resolve("file1.txt"), "content1");
    Files.writeString(sourceDir.resolve("file2.txt"), "content2");

    GitContextMock.GitChange change = new GitContextMock.GitChange(sourceDir, Path.of("targetDir"));
    GitContextMock.GitCommit commit = new GitContextMock.GitCommit(change);
    mock.addChanges(repository, commit);

    mock.pull(repository);

    assertThat(repository.resolve("targetDir")).isDirectory();
    assertThat(repository.resolve("targetDir").resolve("file1.txt")).hasContent("content1");
    assertThat(repository.resolve("targetDir").resolve("file2.txt")).hasContent("content2");
  }

  @Test
  void testSaveCurrentCommitIdResolvesHeadRef(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path gitFolder = repository.resolve(GitContext.GIT_FOLDER);
    Path refFile = gitFolder.resolve("refs").resolve("heads").resolve(MAIN_BRANCH);
    String expectedCommitId = "abc123def456";
    Files.writeString(refFile, expectedCommitId);

    Path trackedFile = tempDir.resolve("tracked-commit.txt");

    mock.saveCurrentCommitId(repository, trackedFile);

    assertThat(trackedFile).exists().hasContent(expectedCommitId);
  }

  @Test
  void testSaveCurrentCommitIdWithDirectCommitId(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    Path gitFolder = repository.resolve(GitContext.GIT_FOLDER);
    Files.createDirectories(gitFolder);

    String directCommitId = "direct123commit456";
    Files.writeString(gitFolder.resolve(GitContext.FILE_HEAD), directCommitId);

    Path trackedFile = tempDir.resolve("tracked.txt");

    mock.saveCurrentCommitId(repository, trackedFile);

    assertThat(trackedFile).exists().hasContent(directCommitId);
  }

  @Test
  void testSaveCurrentCommitIdDoesNotWriteWhenRefNotFound(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    Path gitFolder = repository.resolve(GitContext.GIT_FOLDER);
    Files.createDirectories(gitFolder);

    Files.writeString(gitFolder.resolve(GitContext.FILE_HEAD), "ref: refs/heads/nonexistent");

    Path trackedFile = tempDir.resolve("tracked.txt");

    mock.saveCurrentCommitId(repository, trackedFile);

    assertThat(trackedFile).doesNotExist();
  }

  @Test
  void testDetermineCurrentBranchAfterClone(@TempDir Path tempDir) {

    GitContextMock mock = newGitContextMock(tempDir);
    GitUrl gitUrl = new GitUrl(TEST_URL, TEST_BRANCH);
    Path repository = tempDir.resolve("repo");
    mock.clone(gitUrl, repository);

    String branch = mock.determineCurrentBranch(repository);

    assertThat(branch).isEqualTo(TEST_BRANCH);
  }

  @Test
  void testRetrieveGitUrlFromConfig(@TempDir Path tempDir) {

    GitContextMock mock = newGitContextMock(tempDir);
    GitUrl gitUrl = new GitUrl(TEST_URL, null);
    Path repository = tempDir.resolve("repo");
    mock.clone(gitUrl, repository);

    String url = mock.retrieveGitUrl(repository);

    assertThat(url).isEqualTo(TEST_URL);
  }

  @Test
  void testIsRepositoryUpdateAvailableInitiallyFalse(@TempDir Path tempDir) {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    boolean result = mock.isRepositoryUpdateAvailable(repository);

    assertThat(result).isFalse();
  }

  @Test
  void testIsRepositoryUpdateAvailableWithTrackedCommitId(@TempDir Path tempDir) throws IOException {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path trackedFile = tempDir.resolve("tracked-commit.txt");
    mock.saveCurrentCommitId(repository, trackedFile);

    Path sourceFile = tempDir.resolve("source.txt");
    Files.writeString(sourceFile, "content");

    GitContextMock.GitChange change = new GitContextMock.GitChange(sourceFile, Path.of("file.txt"));
    GitContextMock.GitCommit commit = new GitContextMock.GitCommit(change);
    mock.addChanges(repository, commit);

    mock.fetch(repository, GitContext.DEFAULT_REMOTE, MAIN_BRANCH);

    boolean result = mock.isRepositoryUpdateAvailable(repository, trackedFile);

    assertThat(result).isTrue();
  }

  @Test
  void testIsRepositoryUpdateAvailableWithTrackedCommitIdNoUpdate(@TempDir Path tempDir) {

    GitContextMock mock = newGitContextMock(tempDir);
    Path repository = tempDir.resolve("repo");
    mock.clone(GitUrl.of(TEST_URL), repository);

    Path trackedFile = tempDir.resolve("tracked-commit.txt");
    mock.saveCurrentCommitId(repository, trackedFile);

    boolean result = mock.isRepositoryUpdateAvailable(repository, trackedFile);

    assertThat(result).isFalse();
  }
}
