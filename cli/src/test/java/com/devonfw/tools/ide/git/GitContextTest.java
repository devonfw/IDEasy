package com.devonfw.tools.ide.git;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.ProcessContextGitMock;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;
import com.devonfw.tools.ide.process.OutputMessage;

/**
 * Test of {@link GitContext}.
 */
public class GitContextTest extends AbstractIdeContextTest {

  private static final String CONTENT_ORIGINAL = "original";
  private static final String CONTENT_CHANGED = "changed";

  private ProcessContextGitMock processContext;

  private IdeTestContext newGitContext(Path dir) {

    IdeTestContext context = newContext(dir);
    context.getNetworkStatus().simulateOnline();
    this.processContext = new ProcessContextGitMock(context, dir);
    context.setProcessContext(processContext);
    context.setGitContext(new GitContextImpl(context));
    return context;
  }

  /**
   * Runs a git clone in offline mode and expects an exception to be thrown with a message.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitCloneInOfflineModeThrowsException(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    IdeTestContext context = newGitContext(tempDir);
    GitUrl gitUrl = new GitUrl(gitRepoUrl, "branch");
    context.getStartContext().setOfflineMode(true);

    // act
    CliException e1 = assertThrows(CliException.class, () -> {
      context.getGitContext().pullOrClone(gitUrl, tempDir);
    });
    // assert
    assertThat(e1).hasMessageContaining(gitRepoUrl).hasMessage("You are offline but Internet access is required for git clone of " + gitUrl)
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
    IdeTestContext context = newGitContext(tempDir);
    OutputMessage outputMessage = new OutputMessage(false, "test-remote");
    this.processContext.addOutputMessage(outputMessage);
    // act
    context.getGitContext().pullOrClone(GitUrl.of(gitRepoUrl), tempDir);
    // assert
    assertThat(tempDir.resolve(GitContext.GIT_FOLDER).resolve("url")).hasContent(gitRepoUrl);
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
    IdeTestContext context = newGitContext(tempDir);
    OutputMessage outputMessage = new OutputMessage(false, "test-remote");
    this.processContext.addOutputMessage(outputMessage);
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(GitContext.GIT_FOLDER);
    fileAccess.mkdirs(gitFolderPath);
    // act
    context.getGitContext().pullOrClone(GitUrl.of(gitRepoUrl), tempDir);
    // assert
    assertThat(tempDir.resolve(GitContext.GIT_FOLDER).resolve("update")).hasContent(this.processContext.getNow().toString());
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
    IdeTestContext context = newGitContext(tempDir);
    Path modifiedFile = prepareGit(tempDir, context);
    // act
    context.getGitContext().pullOrCloneAndResetIfNeeded(new GitUrl(gitRepoUrl, "master"), tempDir, "origin");
    // assert
    assertThat(modifiedFile).hasContent(CONTENT_ORIGINAL);
  }

  private static Path prepareGit(Path tempDir, IdeTestContext context) {
    FileAccess fileAccess = context.getFileAccess();
    Path gitFolderPath = tempDir.resolve(GitContext.GIT_FOLDER);
    Path objects = gitFolderPath.resolve("objects");
    fileAccess.mkdirs(objects);
    Path referenceFile = objects.resolve("referenceFile");
    Path modifiedFile = tempDir.resolve("trackedFile");
    fileAccess.touch(gitFolderPath.resolve(GitContext.FILE_HEAD));
    fileAccess.writeFileContent(CONTENT_ORIGINAL, referenceFile);
    fileAccess.writeFileContent(CONTENT_CHANGED, modifiedFile);
    return modifiedFile;
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
    IdeTestContext context = newGitContext(tempDir);
    OutputMessage outputMessage = new OutputMessage(false, "test-remote");
    this.processContext.addOutputMessage(outputMessage);
    GitContext gitContext = context.getGitContext();
    FileAccess fileAccess = context.getFileAccess();
    Path gitFolderPath = tempDir.resolve(GitContext.GIT_FOLDER);
    fileAccess.mkdirs(gitFolderPath);
    fileAccess.mkdirs(tempDir.resolve("new-folder"));
    try {
      Files.createFile(gitFolderPath.resolve("HEAD"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // act
    gitContext.pullOrCloneAndResetIfNeeded(GitUrl.ofMain(gitRepoUrl), tempDir, "origin");
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
    IdeTestContext context = newGitContext(tempDir);
    context.getStartContext().setOfflineMode(true);
    GitContext gitContext = context.getGitContext();

    // act
    gitContext.fetchIfNeeded(tempDir, repoUrl, remoteName);

    // assert
    assertThat(this.processContext.getResults()).isEmpty();
  }

  /**
   * Test for fetchIfNeeded when the system is online.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testFetchIfNeededOnline(@TempDir Path tempDir) {
    // arrange
    String repoUrl = "https://github.com/test";
    String remoteName = "origin";
    IdeTestContext context = newGitContext(tempDir);
    GitContext gitContext = context.getGitContext();

    // act
    gitContext.fetchIfNeeded(tempDir, repoUrl, remoteName);

    // assert
    assertThat(this.processContext.getResults()).hasSize(1);
    assertThat(this.processContext.getResults().getFirst().getCommand()).isEqualTo("git fetch " + repoUrl + " " + remoteName);
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
    IdeContext context = newGitContext(tempDir);
    GitContext gitContext = context.getGitContext();

    Path gitDir = tempDir.resolve(GitContext.GIT_FOLDER);
    Files.createDirectories(gitDir);
    Path fetchHead = gitDir.resolve(GitContext.FILE_FETCH_HEAD);
    Files.createFile(fetchHead);
    Files.setLastModifiedTime(fetchHead, FileTime.fromMillis(System.currentTimeMillis()));

    // act
    gitContext.fetchIfNeeded(tempDir, repoUrl, remoteName);

    // assert
    assertThat(this.processContext.getResults()).isEmpty();
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
    IdeContext context = newGitContext(tempDir);
    GitContext gitContext = new GitContextImpl(context);

    // act
    boolean result = gitContext.isRepositoryUpdateAvailable(tempDir);

    // assert
    assertThat(result).isFalse(); // No updates should be available
  }


  /**
   * Runs a simulated git rest.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  public void testRunGitReset(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    IdeTestContext context = newGitContext(tempDir);
    Path modifiedFile = prepareGit(tempDir, context);
    assertThat(modifiedFile).hasContent(CONTENT_CHANGED);
    // act
    context.getGitContext().reset(tempDir);
    // assert
    assertThat(modifiedFile).hasContent(CONTENT_ORIGINAL);
  }
}
