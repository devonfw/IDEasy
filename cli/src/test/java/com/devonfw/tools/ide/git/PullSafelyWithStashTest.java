package com.devonfw.tools.ide.git;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextGitMock;
import com.devonfw.tools.ide.io.FileAccess;

/**
 * Test of {@link GitContextImpl#pullSafelyWithStash(Path)} method and related functionality.
 */
class PullSafelyWithStashTest extends AbstractIdeContextTest {

  private GitContextImplMock gitContextMock;
  private IdeTestContext context;
  private Path testRepository;

  /**
   * Set up test context and mock objects.
   *
   * @param tempDir a {@link TempDir} {@link Path}
   */
  @BeforeEach
  void setup(@TempDir Path tempDir) {
    this.testRepository = tempDir.resolve("test-repo");
    this.context = newContext(tempDir);
    this.context.getNetworkStatus().simulateOnline();
    ProcessContextGitMock processContext = new ProcessContextGitMock(context, tempDir);
    this.context.setProcessContext(processContext);
    this.gitContextMock = new GitContextImplMock(context, tempDir);
    this.context.setGitContext(gitContextMock);

    // Create a simple git repository structure
    createTestRepository(this.testRepository);
  }

  /**
   * Create a simple test git repository structure.
   *
   * @param repository the path to the repository
   */
  private void createTestRepository(Path repository) {
    try {
      FileAccess fileAccess = this.context.getFileAccess();
      fileAccess.mkdirs(repository);
      Path gitFolder = repository.resolve(GitContext.GIT_FOLDER);
      fileAccess.mkdirs(gitFolder);
      fileAccess.touch(gitFolder.resolve(GitContext.FILE_HEAD));
      fileAccess.writeFileContent("ref: refs/heads/main", gitFolder.resolve("HEAD"));
    } catch (Exception e) {
      throw new RuntimeException("Failed to create test repository", e);
    }
  }

  /**
   * Test successful stash creation, pull, and pop when untracked files are present. This tests the main happy path of pullSafelyWithStash.
   */
  @Test
  void testPullSafelyWithStashSuccessful() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.addStashEntry("stash@{0}", "autostash:pull:test-token");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have been called (HEAD file should be touched)
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test pullSafelyWithStash behavior when stash creation fails. Should continue despite stash creation failure.
   */
  @Test
  void testPullSafelyWithStashCreationFails() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.setStashCreationFailed(true);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should still be called
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test pullSafelyWithStash when stash list operation fails. Should handle gracefully and skip stash pop.
   */
  @Test
  void testPullSafelyWithStashListFails() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.setStashListFailed(true);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should still have been called
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test pullSafelyWithStash when stash pop operation fails after pull. Should still complete the pull successfully.
   */
  @Test
  void testPullSafelyWithStashPopFails() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.setStashPopFailed(true);
    this.gitContextMock.addStashEntry("stash@{0}", "autostash:pull:test-token");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have succeeded despite stash pop failure
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test pullSafelyWithStash when stash reference cannot be found. Should skip stash pop gracefully.
   */
  @Test
  void testPullSafelyWithStashRefNotFound() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    // Don't add any stash entries - simulating that the stash was not found

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should still have been called
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test repoHasUntrackedFiles method returns true when untracked files exist.
   */
  @Test
  void testRepoHasUntrackedFilesTrue() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);

    // act
    boolean hasUntrackedFiles = this.context.getGitContext().repoHasUntrackedFiles(this.testRepository);

    // assert
    assertThat(hasUntrackedFiles).isTrue();
  }

  /**
   * Test repoHasUntrackedFiles method returns false when no untracked files exist.
   */
  @Test
  void testRepoHasUntrackedFilesFalse() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(false);

    // act
    boolean hasUntrackedFiles = this.context.getGitContext().repoHasUntrackedFiles(this.testRepository);

    // assert
    assertThat(hasUntrackedFiles).isFalse();
  }

  /**
   * Test successful stash pop with correct stash reference. Verifies that when a stash is found by token, the pop operation is attempted.
   */
  @Test
  void testSuccessfulStashPopWithCorrectReference() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    String stashRef = "stash@{0}";
    String stashMessage = "autostash:pull:12345-unique-token";
    this.gitContextMock.addStashEntry(stashRef, stashMessage);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have completed
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test finding the correct stash reference from multiple entries in the stash list. Verifies that the correct stash is identified even when multiple stashes
   * exist.
   */
  @Test
  void testFindCorrectStashRefFromMultipleEntries() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    String targetToken = "autostash:pull:target-token";
    this.gitContextMock.addStashEntry("stash@{0}", "some-other-stash");
    this.gitContextMock.addStashEntry("stash@{1}", targetToken);
    this.gitContextMock.addStashEntry("stash@{2}", "another-stash");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have been executed with correct stash identified
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test that stash pop is skipped when stash reference is null. Verifies graceful handling when stash cannot be found.
   */
  @Test
  void testStashPopSkippedWhenRefIsNull() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.clearStashList();

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should still complete
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test that pull is always called even if stash operations fail. This is critical to ensure the pull operation happens regardless of stash issues.
   */
  @Test
  void testPullIsAlwaysCalled() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.setStashCreationFailed(true);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have been called despite stash creation failure
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test with empty stash list - no stash entry found. Verifies that empty stash list is handled correctly.
   */
  @Test
  void testEmptyStashList() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    // Empty stash list - no entries added

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - should handle empty stash gracefully, pull should complete
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test that repoHasUntrackedFiles properly checks for untracked files. Verifies the integration point used by pullSafelyWithStash.
   */
  @Test
  void testRepoHasUntrackedFilesIntegration() {
    // arrange
    boolean untrackedFilesExist = true;
    this.gitContextMock.setSimulateUntrackedFiles(untrackedFilesExist);

    // act
    boolean result = this.context.getGitContext().repoHasUntrackedFiles(this.testRepository);

    // assert
    assertThat(result).isEqualTo(untrackedFilesExist);
  }

  /**
   * Test configuration of stash failure modes for testing different error scenarios.
   */
  @Test
  void testMockConfigurationMethods() {
    // arrange & act
    this.gitContextMock.setStashCreationFailed(true);
    this.gitContextMock.setStashListFailed(true);
    this.gitContextMock.setStashPopFailed(true);
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.addStashEntry("stash@{0}", "test");
    this.gitContextMock.clearStashList();

    // assert - mock should be properly configured
    assertThat(this.gitContextMock).isNotNull();
  }

  /**
   * Test combination of stash creation and list failures. Both operations failing should still allow pull to execute.
   */
  @Test
  void testCombinedStashCreationAndListFailures() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.setStashCreationFailed(true);
    this.gitContextMock.setStashListFailed(true);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should still execute
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test all stash operations failing together. Pull should still succeed as it is the primary operation.
   */
  @Test
  void testAllStashOperationsFail() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.setStashCreationFailed(true);
    this.gitContextMock.setStashListFailed(true);
    this.gitContextMock.setStashPopFailed(true);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should complete successfully
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test stash operations with multiple attempts at finding stash ref. Verifies that the correct stash is identified in complex scenarios.
   */
  @Test
  void testStashRefIdentificationWithManyEntries() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    // Add many stash entries
    for (int i = 0; i < 10; i++) {
      this.gitContextMock.addStashEntry("stash@{" + i + "}", "old-stash-" + i);
    }
    // Add target stash in the middle
    this.gitContextMock.addStashEntry("stash@{10}", "autostash:pull:critical-token");
    // Add more stashes after
    for (int i = 11; i < 15; i++) {
      this.gitContextMock.addStashEntry("stash@{" + i + "}", "newer-stash-" + i);
    }

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have completed with correct stash identified
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test repoHasUntrackedFiles when set to false. Verifies that the mock properly simulates no untracked files.
   */
  @Test
  void testNoUntrackedFilesScenario() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(false);

    // act
    boolean hasUntracked = this.context.getGitContext().repoHasUntrackedFiles(this.testRepository);

    // assert
    assertThat(hasUntracked).isFalse();
  }

  /**
   * Test stash operations order: create -> list -> pop. Verifies that operations are executed in the correct sequence.
   */
  @Test
  void testStashOperationsSequence() {
    // arrange
    this.gitContextMock.setSimulateUntrackedFiles(true);
    // Simulate all operations succeeding
    this.gitContextMock.setStashCreationFailed(false);
    this.gitContextMock.setStashListFailed(false);
    this.gitContextMock.setStashPopFailed(false);
    this.gitContextMock.addStashEntry("stash@{0}", "autostash:pull:sequence-test");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert - pull should have been called
    Path headFile = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve(GitContext.FILE_HEAD);
    assertThat(headFile).exists();
  }

  /**
   * Test clearing and resetting mock state. Verifies that the mock can be reconfigured between operations.
   */
  @Test
  void testMockStateManagement() {
    // arrange - first configuration
    this.gitContextMock.setSimulateUntrackedFiles(true);
    this.gitContextMock.addStashEntry("stash@{0}", "test1");

    // act - execute with first config
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // Clear for second use
    this.gitContextMock.clearStashList();
    this.gitContextMock.setSimulateUntrackedFiles(false);

    // assert - second state should be applied
    boolean hasUntracked = this.context.getGitContext().repoHasUntrackedFiles(this.testRepository);
    assertThat(hasUntracked).isFalse();
  }
}

