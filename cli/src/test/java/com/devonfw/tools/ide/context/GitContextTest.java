package com.devonfw.tools.ide.context;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;

/**
 * Test of {@link GitContext}.
 */
public class GitContextTest extends AbstractIdeContextTest {

  private ProcessContextGitMock processContext;

  private IdeTestContext newGitContext(Path dir) {

    IdeTestContext context = newContext(dir);
    context.setOnline(Boolean.TRUE);
    this.processContext = new ProcessContextGitMock(dir);
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
    this.processContext.getOuts().add("test-remote");
    context.setOnline(Boolean.FALSE);

    //IdeContext context = newGitContext(tempDir, errors, outs, 0, false);
    // act
    CliException e1 = assertThrows(CliException.class, () -> {
      context.getGitContext().pullOrClone(gitRepoUrl, "", tempDir);
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
    IdeTestContext context = newGitContext(tempDir);
    this.processContext.getOuts().add("test-remote");
    // act
    context.getGitContext().pullOrClone(gitRepoUrl, tempDir);
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
    IdeTestContext context = newGitContext(tempDir);
    this.processContext.getOuts().add("test-remote");
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    // act
    context.getGitContext().pullOrClone(gitRepoUrl, tempDir);
    // assert
    assertThat(tempDir.resolve(".git").resolve("update")).hasContent(this.processContext.getNow().toString());
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
    IdeTestContext context = newGitContext(tempDir);
    this.processContext.getOuts().add("test-remote");
    // act
    context.getGitContext().pullOrCloneAndResetIfNeeded(gitRepoUrl, tempDir, "master", "origin");
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
    IdeTestContext context = newGitContext(tempDir);
    this.processContext.getOuts().add("test-remote");
    GitContext gitContext = context.getGitContext();
    FileAccess fileAccess = context.getFileAccess();
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

}
