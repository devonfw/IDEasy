package com.devonfw.tools.ide.context;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;

/**
 * Test of {@link GitContext}.
 */
public class GitContextTest extends AbstractIdeContextTest {

  /**
   * Runs a git clone in offline mode and expects an exception to be thrown with a message.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitCloneInOfflineModeThrowsException(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitContext(tempDir, errors, outs, 0, false);
    GitContext gitContext = new GitContextImpl(context);
    // act
    CliException e1 = assertThrows(CliException.class, () -> {
      gitContext.pullOrClone(gitRepoUrl, "", tempDir);
    });
    // assert
    assertThat(e1).hasMessageContaining(gitRepoUrl).hasMessageContaining(tempDir.toString())
        .hasMessageContaining("offline");

  }

  /**
   * Runs a simulated git clone and checks if a new file with the correct repository URL was created.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitClone(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);
    // act
    gitContext.pullOrClone(gitRepoUrl, tempDir);
    // assert
    assertThat(tempDir.resolve(".git").resolve("url")).hasContent(gitRepoUrl);
  }

  /**
   * Runs a simulated git pull without force mode, checks if a new file with the current date was created.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitPullWithoutForce(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);
    Date currentDate = new Date();
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    // act
    gitContext.pullOrClone(gitRepoUrl, tempDir);
    // assert
    assertThat(tempDir.resolve(".git").resolve("update")).hasContent(currentDate.toString());
  }

  /**
   * Runs a git pull with force mode, creates temporary files to simulate a proper cleanup.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitPullWithForceStartsReset(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    Path gitFolderPath = tempDir.resolve(".git");
    try {
      Files.createDirectory(gitFolderPath);
      Files.createDirectory(gitFolderPath.resolve("objects"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Path referenceFile;
    Path modifiedFile;
    try {
      Files.createFile(gitFolderPath.resolve("HEAD"));
      referenceFile = Files.createFile(gitFolderPath.resolve("objects").resolve("referenceFile"));
      Files.writeString(referenceFile, "original");
      modifiedFile = Files.createFile(tempDir.resolve("trackedFile"));
      Files.writeString(modifiedFile, "changed");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);
    // act
    gitContext.pullOrCloneAndResetIfNeeded(gitRepoUrl, tempDir, "master", "origin");
    // assert
    assertThat(modifiedFile).hasContent("original");
  }

  /**
   * Runs a git pull with force and starts a cleanup (checks if an untracked folder was removed).
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitPullWithForceStartsCleanup(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    fileAccess.mkdirs(tempDir.resolve("new-folder"));
    try {
      Files.createFile(gitFolderPath.resolve("HEAD"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // act
    gitContext.pullOrCloneAndResetIfNeeded(gitRepoUrl, tempDir, "master", "origin");
    // assert
    assertThat(tempDir.resolve("new-folder")).doesNotExist();
  }

  /**
   * Test for fetchIfNeeded when the system is offline.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testFetchIfNeededOffline(@TempDir Path tempDir) {
    // arrange
    String repoUrl = "https://github.com/test";
    String remoteName = "origin";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    IdeContext context = newGitContext(tempDir, errors, outs, 0, false);
    GitContext gitContext = new GitContextImpl(context);

    // act
    gitContext.fetchIfNeeded(repoUrl, remoteName, tempDir);

    // assert
    // Since the context is offline, no actions should be performed
    assertThat(errors.isEmpty()).isTrue();
    assertThat(outs.isEmpty()).isTrue();
  }

  /**
   * Test for fetchIfNeeded when the FETCH_HEAD file is newer than the threshold.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testFetchIfNeededRecentFetchHead(@TempDir Path tempDir) throws IOException {
    // arrange
    String repoUrl = "https://github.com/test";
    String remoteName = "origin";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);

    Path gitDir = tempDir.resolve(".git");
    Files.createDirectories(gitDir);
    Path fetchHead = gitDir.resolve("FETCH_HEAD");
    Files.createFile(fetchHead);
    Files.setLastModifiedTime(fetchHead, FileTime.fromMillis(System.currentTimeMillis()));

    // act
    gitContext.fetchIfNeeded(repoUrl, remoteName, tempDir);

    // assert
    // Since FETCH_HEAD is recent, no fetch should occur
    assertThat(errors.isEmpty()).isTrue();
    assertThat(outs.isEmpty()).isTrue();
  }

  /**
   * Test for fetchIfNeeded when the FETCH_HEAD file is older than the threshold and updates are available.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testFetchIfNeededOldFetchHeadWithUpdates(@TempDir Path tempDir) throws IOException {
    // arrange
    String repoUrl = "https://github.com/test";
    String remoteName = "origin";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);

    Path gitDir = tempDir.resolve(".git");
    Files.createDirectories(gitDir);
    Path fetchHead = gitDir.resolve("FETCH_HEAD");
    Files.createFile(fetchHead);
    Files.setLastModifiedTime(fetchHead, FileTime.fromMillis(System.currentTimeMillis() - 86400000)); // 1 day old

    // Mocking repository update availability
    outs.add("new updates available");

    // act
    gitContext.fetchIfNeeded(repoUrl, remoteName, tempDir);

    // assert
    // Since FETCH_HEAD is old and updates are available, fetch should occur
    assertThat(outs.isEmpty()).isFalse();
  }

  /**
   * Test for isRepositoryUpdateAvailable when local and remote commits are the same.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testIsRepositoryUpdateAvailableNoUpdates(@TempDir Path tempDir) {
    // arrange
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("local_commit_hash");
    outs.add("local_commit_hash"); // same as remote to simulate no updates
    IdeContext context = newGitContext(tempDir, errors, outs, 0, true);
    GitContext gitContext = new GitContextImpl(context);

    // act
    boolean result = gitContext.isRepositoryUpdateAvailable(tempDir, "origin", "test_branch");

    // assert
    assertThat(result).isFalse(); // No updates should be available
  }
}
