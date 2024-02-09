package com.devonfw.tools.ide.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;

/**
 * Test of {@link GitUtils}.
 */
public class GitUtilsTest extends AbstractIdeContextTest {

  /**
   * Runs a git clone in offline mode and expects an exception to be thrown with a message.
   */
  @Test
  public void testRunGitCloneInOfflineModeThrowsException(@TempDir Path tempDir) {

    // arrange
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, false);
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");

    CliException e1 = assertThrows(CliException.class, () -> {
      gitUtils.runGitPullOrClone(true, "https://github.com/test");
    });
    assertThat(e1).hasMessageContaining("https://github.com/test").hasMessageContaining(tempDir.toString())
        .hasMessageContaining("offline");

  }

  /**
   * Runs a simulated git clone and checks if a new file with the correct repository URL was created.
   */
  @Test
  public void testRunGitClone(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, true);
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    // act
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
    // assert
    assertThat(tempDir.resolve(".git").resolve("url")).hasContent(gitRepoUrl);
  }

  /**
   * Runs a simulated git pull without force mode, checks if a new file with the current date was created.
   */
  @Test
  public void testRunGitPullWithoutForce(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, true);
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    Date currentDate = new Date();
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    gitUtils.runGitPullOrClone(false, gitRepoUrl);
    // assert
    assertThat(tempDir.resolve(".git").resolve("update")).hasContent(currentDate.toString());
  }

  /**
   * Runs a git pull with force mode, creates temporary files to simulate a proper cleanup.
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
      referenceFile = Files.createFile(gitFolderPath.resolve("objects").resolve("referenceFile"));
      Files.writeString(referenceFile, "original");
      modifiedFile = Files.createFile(tempDir.resolve("trackedFile"));
      Files.writeString(modifiedFile, "changed");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, true);

    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    // act
    gitUtils.runGitPullOrClone(true, gitRepoUrl);

    // assert
    assertThat(modifiedFile).hasContent("original");
  }

  /**
   * Runs a git pull with force and starts a cleanup (checks if an untracked folder was removed).
   */
  @Test
  public void testRunGitPullWithForceStartsCleanup(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, true);
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    fileAccess.mkdirs(tempDir.resolve("new-folder"));
    // assert
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
    assertThat(tempDir.resolve("new-folder")).doesNotExist();
  }
}
