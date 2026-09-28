package com.devonfw.tools.ide.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextGitMock;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Test of {@link GitContextImpl#pullSafelyWithStash(Path)} and the related {@link GitContextImpl#hasUntrackedFiles(Path)} method. The real git commands are
 * executed through a {@link ProcessContextGitMock}, which simulates them via file operations so no git installation is required.
 */
class PullSafelyWithStashTest extends AbstractIdeContextTest {

  private static final String UNTRACKED_FILE = "untracked-file.txt";

  private ProcessContextGitMock processContext;
  private TestGitContext gitContext;
  private IdeTestContext context;
  private Path testRepository;

  /**
   * A {@link GitContextImpl} double only overriding the git executable lookup so that the git commands run against the {@link ProcessContextGitMock} without
   * requiring a git installation.
   */
  private class TestGitContext extends GitContextImpl {

    /**
     * @param context the {@link com.devonfw.tools.ide.context.IdeContext context}.
     */
    TestGitContext(IdeContext context) {

      super(context);
    }

    @Override
    public Path findGitRequired() {

      return Path.of("git");
    }
  }

  /**
   * Sets up the test context with a simulated git repository.
   *
   * @param tempDir a {@link TempDir} {@link Path}
   */
  @BeforeEach
  void setup(@TempDir Path tempDir) {
    this.testRepository = tempDir.resolve("test-repo");
    this.context = newContext(tempDir);
    this.context.getNetworkStatus().simulateOnline();
    this.processContext = new ProcessContextGitMock(this.context, this.testRepository);
    this.context.setProcessContext(this.processContext);
    this.gitContext = new TestGitContext(this.context);
    this.context.setGitContext(this.gitContext);

    // create a simple git repository structure
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(this.testRepository);
    Path gitFolder = this.testRepository.resolve(GitContext.GIT_FOLDER);
    fileAccess.mkdirs(gitFolder);
    fileAccess.writeFileContent("ref: refs/heads/main", gitFolder.resolve(GitContext.FILE_HEAD));
  }

  /**
   * Tests the main happy path: untracked files are stashed, the pull is applied and the stash is popped again.
   */
  @Test
  void testPullSafelyWithStashSuccessful() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).containsExactly("git --no-pager stash push --include-untracked -m " + lastStashToken() + " --quiet",
        "git --no-pager stash list", "git --no-pager pull --quiet", "git --no-pager stash pop stash@{0} --quiet");
    assertThat(exitCodes()).containsExactly(0, 0, 0, 0);
    assertThat(this.context.getGitContext().hasUntrackedFiles(this.testRepository)).isTrue();
    assertThat(stashListLines()).isEmpty();
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Test pullSafelyWithStash behavior when stash creation fails. Should continue despite stash creation failure.
   */
  @Test
  void testPullSafelyWithStashCreationFails() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashPushFailed(true);
    this.context.setAnswers("yes", "yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).containsExactly("git --no-pager stash push --include-untracked -m " + lastStashToken() + " --quiet",
        "git --no-pager stash list", "git --no-pager pull --quiet");
    assertThat(exitCodes()).containsExactly(1, 0, 0);
    // assert
    assertThat(this.context.getGitContext().hasUntrackedFiles(this.testRepository)).isTrue();
    assertThat(stashListLines()).isEmpty();
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Tests that a failing stash list is handled gracefully: the pull is still executed and the stash pop is skipped.
   */
  @Test
  void testPullSafelyWithStashListFails() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashListFailed(true);
    this.context.setAnswers("yes", "yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).containsExactly("git --no-pager stash push --include-untracked -m " + lastStashToken() + " --quiet",
        "git --no-pager stash list", "git --no-pager pull --quiet");
    assertThat(exitCodes()).containsExactly(0, 1, 0);
    assertThat(stashListLines()).containsOnly("stash@{0}: " + lastStashToken());
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Tests that a failing stash pop after the pull is handled gracefully: the pull is completed and the stash remains on the stack.
   */
  @Test
  void testPullSafelyWithStashPopFails() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashPopFailed(true);
    this.context.setAnswers("yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).containsExactly("git --no-pager stash push --include-untracked -m " + lastStashToken() + " --quiet",
        "git --no-pager stash list", "git --no-pager pull --quiet", "git --no-pager stash pop stash@{0} --quiet");
    assertThat(exitCodes()).containsExactly(0, 0, 0, 1);
    assertThat(stashListLines()).containsOnly("stash@{0}: " + lastStashToken());
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Tests that a failing pull is handled gracefully: the pull fails but the created stash is popped again so the untracked files are restored.
   */
  @Test
  void testPullSafelyWithStashPullFails() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setPullFailed(true);
    this.context.setAnswers("yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).containsExactly("git --no-pager stash push --include-untracked -m " + lastStashToken() + " --quiet",
        "git --no-pager stash list", "git --no-pager pull --quiet", "git branch --show-current", "git --no-pager stash pop stash@{0} --quiet");
    assertThat(exitCodes()).containsExactly(0, 0, 1, 0, 0);
    assertThat(this.context.getGitContext().hasUntrackedFiles(this.testRepository)).isTrue();
    assertThat(stashListLines()).isEmpty();
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).doesNotExist();
  }

  /**
   * Tests that {@link GitContextImpl#hasUntrackedFiles(Path)} returns {@code true} when untracked files exist.
   */
  @Test
  void testRepoHasUntrackedFilesTrue() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);

    // act
    boolean hasUntrackedFiles = this.context.getGitContext().hasUntrackedFiles(this.testRepository);

    // assert
    assertThat(hasUntrackedFiles).isTrue();
  }

  /**
   * Tests that {@link GitContextImpl#hasUntrackedFiles(Path)} returns {@code false} when no untracked files exist.
   */
  @Test
  void testRepoHasUntrackedFilesFalse() {
    // act
    boolean hasUntrackedFiles = this.context.getGitContext().hasUntrackedFiles(this.testRepository);

    // assert
    assertThat(hasUntrackedFiles).isFalse();
  }

  /**
   * Tests that the created stash is popped using the correct stash reference, even when other stashes already exist. The new stash becomes {@code stash@{0}}
   * and the pre-existing stashes are shifted down by one index.
   */
  @Test
  void testSuccessfulStashPopWithCorrectReference() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.addStashEntry("stash@{1}", "some-other-stash");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert -
    assertThat(usedGitCommands())
        .contains("git --no-pager stash pop stash@{0} --quiet");
    assertThat(stashListLines()).containsOnly("stash@{2}: some-other-stash");
  }

  /**
   * Tests that the correct stash reference is found from multiple stash list entries: the created stash is always the new top entry {@code stash@{0}} and all
   * pre-existing stashes keep their content but shift down by one index.
   */
  @Test
  void testFindCorrectStashRefFromMultipleEntries() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    for (int i = 0; i < 10; i++) {
      this.processContext.addStashEntry("stash@{" + i + "}", "old-stash-" + i);
    }

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert -
    assertThat(usedGitCommands()).contains("git --no-pager stash pop stash@{0} --quiet");
    List<String> lines = stashListLines();
    assertThat(lines).hasSize(10);
    assertThat(lines.get(0)).isEqualTo("stash@{1}: old-stash-0");
    assertThat(lines.get(9)).isEqualTo("stash@{10}: old-stash-9");
  }

  /**
   * Tests that the pull is always called even if the stash creation fails.
   */
  @Test
  void testPullIsAlwaysCalled() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashPushFailed(true);
    this.context.setAnswers("yes", "yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).contains("git --no-pager pull --quiet");
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Tests that an empty stash list is handled gracefully: the stash pop is skipped and the pull is still executed.
   */
  @Test
  void testEmptyStashList() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashPushFailed(true);
    this.context.setAnswers("yes", "yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).hasSize(3).last().isEqualTo("git --no-pager pull --quiet");
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Tests the combination of failing stash creation and failing stash list: the pull is still executed.
   */
  @Test
  void testCombinedStashCreationAndListFailures() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashPushFailed(true);
    this.processContext.setStashListFailed(true);
    this.context.setAnswers("yes", "yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).hasSize(3).last().isEqualTo("git --no-pager pull --quiet");
    assertThat(exitCodes()).containsExactly(1, 1, 0);
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * Test all stash operations failing together. Pull should still succeed as it is the primary operation.
   */
  @Test
  void testAllStashOperationsFail() {
    // arrange
    this.processContext.addUntrackedFile(UNTRACKED_FILE);
    this.processContext.setStashPushFailed(true);
    this.processContext.setStashListFailed(true);
    this.processContext.setStashPopFailed(true);
    this.context.setAnswers("yes", "yes");

    // act
    this.context.getGitContext().pullSafelyWithStash(this.testRepository);

    // assert
    assertThat(usedGitCommands()).hasSize(3).last().isEqualTo("git --no-pager pull --quiet");
    assertThat(exitCodes()).containsExactly(1, 1, 0);
    assertThat(this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("update")).exists();
  }

  /**
   * @return the stash token used by the last executed {@code stash push} command.
   */
  private String lastStashToken() {

    for (ProcessResult result : this.processContext.getResults()) {
      if (result.getCommand().contains("stash push")) {
        String[] args = result.getCommand().split(" ");
        int idx = 0;
        for (idx = 0; idx < args.length - 1; idx++) {
          if (args[idx].equals("-m")) {
            break;
          }
        }
        return args[idx + 1];
      }
    }
    throw new IllegalStateException("No stash push command was executed");
  }

  /**
   * @return the {@link List} of git commands that were executed.
   */
  private List<String> usedGitCommands() {

    return this.processContext.getResults().stream().map(ProcessResult::getCommand).toList();
  }

  /**
   * @return the exit codes of the executed git commands.
   */
  private List<Integer> exitCodes() {

    return this.processContext.getResults().stream().map(ProcessResult::getExitCode).toList();
  }

  /**
   * @return the lines of the simulated stash list.
   */
  private List<String> stashListLines() {

    Path stashList = this.testRepository.resolve(GitContext.GIT_FOLDER).resolve("stash-list");
    if (Files.exists(stashList)) {
      try {
        return List.copyOf(Files.readAllLines(stashList));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return List.of();
  }

}
