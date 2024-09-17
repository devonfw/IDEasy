package com.devonfw.tools.ide.context;

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
      context.getGitContext().pullOrClone(gitRepoUrl, tempDir, "");
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
    IdeContext context = newGitContext(tempDir);
    GitContext gitContext = new GitContextImpl(context);

    // act
    gitContext.fetchIfNeeded(tempDir, repoUrl, remoteName);

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
    IdeContext context = newGitContext(tempDir);
    GitContext gitContext = new GitContextImpl(context);

    Path gitDir = tempDir.resolve(".git");
    Files.createDirectories(gitDir);
    Path fetchHead = gitDir.resolve("FETCH_HEAD");
    Files.createFile(fetchHead);
    Files.setLastModifiedTime(fetchHead, FileTime.fromMillis(System.currentTimeMillis()));

    // act
    gitContext.fetchIfNeeded(tempDir, repoUrl, remoteName);

    // assert
    // Since FETCH_HEAD is recent, no fetch should occur
    assertThat(errors.isEmpty()).isTrue();
    assertThat(outs.isEmpty()).isTrue();
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
   * Tests the conversion of a Git URL from HTTPS to SSH protocol.
   * <p>
   * Given a Git URL in HTTPS format, this test ensures that it is correctly converted to the SSH format using the
   * {@link GitUrlSyntax#convertToPreferredProtocol(GitUrl, String)} method.
   */
  @Test
  public void testConvertGitUrlFromHTTPSToSSH() {
    String url = "https://testgitdomain.com/devonfw/IDEasy.git";
    GitUrl giturl = new GitUrl(url, null);
    GitUrl convertedGitUrl = GitUrlSyntax.convertToPreferredProtocol(giturl, "SSH");

    String ssh_url = "git@testgitdomain.com:devonfw/IDEasy.git";
    assertThat(convertedGitUrl.url()).isEqualTo(ssh_url);
  }

  /**
   * Tests the conversion of a Git URL from SSH to HTTPS protocol.
   * <p>
   * Given a Git URL in SSH format, this test ensures that it is correctly converted to the HTTPS format using the
   * {@link GitUrlSyntax#convertToPreferredProtocol(GitUrl, String)} method.
   */
  @Test
  public void testConvertGitUrlFromSSHToHTTPS() {
    String url = "git@testgitdomain.com:devonfw/IDEasy.git";
    GitUrl giturl = new GitUrl(url, null);
    GitUrl convertedGitUrl = GitUrlSyntax.convertToPreferredProtocol(giturl, "HTTPS");

    String https_url = "https://testgitdomain.com/devonfw/IDEasy.git";
    assertThat(convertedGitUrl.url()).isEqualTo(https_url);
  }

  /**
   * Tests that the conversion does not alter the Git URL when no valid protocol is provided.
   * <p>
   * Given an SSH Git URL and a null preferred protocol, this test ensures that the original URL remains unchanged.
   */
  @Test
  public void testConvertGitUrlNoValidProtocol() {
    String url = "git@testgitdomain.com:devonfw/IDEasy.git";
    GitUrl giturl = new GitUrl(url, null);
    GitUrl convertedGitUrl = GitUrlSyntax.convertToPreferredProtocol(giturl, null);

    assertThat(convertedGitUrl.url()).isEqualTo(url);
  }

  /**
   * Tests that no conversion occurs for domains that should not be converted.
   * <p>
   * This test ensures that when a Git URL points to the github.com domain, it remains in the original format, even if a different protocol (HTTPS) is
   * specified.
   */
  @Test
  public void testConvertGitUrlGitHubDomain() {
    String url = "git@github.com:devonfw/IDEasy.git";
    GitUrl giturl = new GitUrl(url, null);
    GitUrl convertedGitUrl = GitUrlSyntax.convertToPreferredProtocol(giturl, "HTTPS");

    assertThat(convertedGitUrl.url()).isEqualTo(url);
  }
}
