package com.devonfw.tools.ide.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileAccessImpl;

public class GitUtilsTest extends AbstractIdeContextTest {

  /**
   * Runs a git clone in offline mode and expects an exception to be thrown with a message.
   */
  @Test
  public void testRunGitCloneInOfflineModeThrowsException() {

    // arrange
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    String projectName = "gitutils";
    String projectPathName = "urls";
    outs.add("test-remote");
    Path projectPath = Paths.get("target/test-projects").resolve(projectName).resolve(projectPathName);
    IdeContext context = newGitUtilsContext(projectName, projectPathName, true, errors, outs, 0, true);
    System.out.println("testRunGitCloneInOfflineModeThrowsException");
    GitUtils gitUtils = new GitUtils(context, projectPath, "origin", "master");
    try {
      // act
      gitUtils.runGitPullOrClone(true, "https://github.com/test");
    } catch (Exception e) {
      // assert
      assertThat(e).isInstanceOf(CliException.class);
      assertThat(e).hasMessageContaining("https://github.com/test").hasMessageContaining(projectPath.toString())
          .hasMessageContaining("offline");
    }
  }

  /**
   * Runs a simulated git clone and checks if a new file with the correct repository URL was created.
   */
  @Test
  public void testRunGitClone() {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    String projectName = "gitutils";
    String projectPathName = "urls";
    outs.add("test-remote");
    Path projectPath = Paths.get("target/test-projects").resolve(projectName).resolve(projectPathName);
    IdeContext context = newGitUtilsContext(projectName, projectPathName, true, errors, outs, 0, false);
    System.out.println("testRunGitClone");
    GitUtils gitUtils = new GitUtils(context, projectPath, "origin", "master");
    // act
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
    // assert
    assertThat(projectPath.resolve(".git").resolve("status").resolve("url")).hasContent(gitRepoUrl);
  }

  /**
   * Runs a simulated git pull without force mode, checks if a new file with the current date was created.
   */
  @Test
  public void testRunGitPullWithoutForce() {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();

    String projectName = "gitutils";
    String projectPathName = "urls";
    outs.add("test-remote");
    Path projectPath = Paths.get("target/test-projects").resolve(projectName).resolve(projectPathName);
    IdeContext context = newGitUtilsContext(projectName, projectPathName, true, errors, outs, 0, false);
    System.out.println("testRunGitPullWithoutForce");
    GitUtils gitUtils = new GitUtils(context, projectPath, "origin", "master");
    Date currentDate = new Date();
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = projectPath.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    gitUtils.runGitPullOrClone(false, gitRepoUrl);
    // assert
    assertThat(projectPath.resolve(".git").resolve("status").resolve("update")).hasContent(currentDate.toString());
  }

  /**
   * Runs a git pull with force mode, creates temporary files to simulate a proper cleanup.
   */
  @Test
  public void testRunGitPullWithForce() {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    String projectName = "gitutils";
    String projectPathName = "urls";
    outs.add("test-remote");
    Path projectPath = Paths.get("target/test-projects").resolve(projectName).resolve(projectPathName);
    IdeContext context = newGitUtilsContext(projectName, projectPathName, true, errors, outs, 0, true);
    System.out.println("testRunGitPullWithForce");
    GitUtils gitUtils = new GitUtils(context, projectPath, "origin", "master");
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = projectPath.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);

    // assert
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
  }

  @Test
  public void testRunGitPullWithForceStartsCleanup() {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    String projectName = "gitutils";
    String projectPathName = "urls";
    outs.add("test-remote");
    Path projectPath = Paths.get("target/test-projects").resolve(projectName).resolve(projectPathName);
    IdeContext context = newGitUtilsContext(projectName, projectPathName, true, errors, outs, 0, false);
    System.out.println("testRunGitPullWithForceStartsCleanup");
    GitUtils gitUtils = new GitUtils(context, projectPath, "origin", "master");
    // act
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = projectPath.resolve(".git");
    fileAccess.mkdirs(gitFolderPath);
    fileAccess.mkdirs(projectPath.resolve("new-folder"));
    // assert
    gitUtils.runGitPullOrClone(true, gitRepoUrl);
    assertThat(projectPath.resolve("new-folder")).doesNotExist();
  }
}
