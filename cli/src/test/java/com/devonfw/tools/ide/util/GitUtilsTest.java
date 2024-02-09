package com.devonfw.tools.ide.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
    System.out.println("testRunGitCloneInOfflineModeThrowsException");
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
    System.out.println("testRunGitClone");
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    // act
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
    // assert
    assertThat(tempDir.resolve(".git").resolve("status").resolve("url")).hasContent(gitRepoUrl);
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
    System.out.println("testRunGitPullWithoutForce");
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    Date currentDate = new Date();
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    gitUtils.runGitPullOrClone(false, gitRepoUrl);
    // assert
    assertThat(tempDir.resolve(".git").resolve("status").resolve("update")).hasContent(currentDate.toString());
  }

  /**
   * Runs a git pull with force mode, creates temporary files to simulate a proper cleanup.
   */
  @Test
  public void testRunGitPullWithForce(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, false);
    System.out.println("testRunGitPullWithForce");
    GitUtils gitUtils = new GitUtils(context, tempDir, "origin", "master");
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);

    // assert
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
  }

  @Test
  public void testRunGitPullWithForceStartsCleanup(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("test-remote");
    IdeContext context = newGitUtilsContext(tempDir, errors, outs, 0, true);
    System.out.println("testRunGitPullWithForceStartsCleanup");
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
