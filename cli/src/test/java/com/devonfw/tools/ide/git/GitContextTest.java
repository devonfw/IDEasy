package com.devonfw.tools.ide.git;

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
class GitContextTest extends AbstractIdeContextTest {

  private static final String CONTENT_ORIGINAL = "original";
  private static final String CONTENT_CHANGED = "changed";

  private ProcessContextGitMock processContext;

  /**
   * Extra GitContextImpl class with disabled verifyGitInstalled method.
   */
  private class GitContextMock extends GitContextImpl {

    /** Simulates the remote the current branch tracks via {@code branch.<branch>.remote}; {@code null} simulates a branch without upstream. */
    private String trackedRemote = DEFAULT_REMOTE;

    /**
     * @param context the {@link IdeContext context}.
     */
    public GitContextMock(IdeContext context) {
      super(context);
    }

    @Override
    public Path findGitRequired() {
      return Path.of("git");
    }

    @Override
    public String determineTrackedRemote(Path repository) {
      return this.trackedRemote;
    }

    /**
     * @param trackedRemote the remote the current branch tracks, or {@code null} to simulate a branch without upstream.
     */
    public void setTrackedRemote(String trackedRemote) {
      this.trackedRemote = trackedRemote;
    }
  }

  private GitContextMock gitContextMock;

  private IdeTestContext newGitContext(Path dir) {

    IdeTestContext context = newContext(dir);
    context.getNetworkStatus().simulateOnline();
    this.processContext = new ProcessContextGitMock(context, dir);
    context.setProcessContext(processContext);
    this.gitContextMock = new GitContextMock(context);
    context.setGitContext(this.gitContextMock);
    return context;
  }

  /**
   * Runs a git clone in offline mode and expects an exception to be thrown with a message.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testRunGitCloneInOfflineModeThrowsException(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    IdeTestContext context = newGitContext(tempDir);
    GitUrl gitUrl = new GitUrl(gitRepoUrl, "branch");
    context.getStartContext().setOfflineMode(true);

    // act
    CliException e1 = catchThrowableOfType(CliException.class, () -> {
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
  void testRunGitClone(@TempDir Path tempDir) {

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
  void testRunGitPullWithoutForce(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    IdeTestContext context = newGitContext(tempDir);
    FileAccess fileAccess = new FileAccessImpl(context);
    Path gitFolderPath = tempDir.resolve(GitContext.GIT_FOLDER);
    fileAccess.mkdirs(gitFolderPath);
    // act
    context.getGitContext().pullOrClone(GitUrl.of(gitRepoUrl), tempDir);
    // assert
    assertThat(tempDir.resolve(GitContext.GIT_FOLDER).resolve("update")).hasContent(this.processContext.getNow().toString());
  }

  /**
   * Runs a simulated git pull on a repository whose current branch tracks a non-default remote (as it would in a repository with multiple remotes) and checks
   * that the pull is performed instead of asking the user to continue. See <a href="https://github.com/devonfw/IDEasy/issues/840">issue #840</a>.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testRunGitPullWithTrackedRemote(@TempDir Path tempDir) {

    // arrange - a non-default tracked remote, as in a repository with multiple remotes
    String gitRepoUrl = "https://github.com/test";
    IdeTestContext context = newGitContext(tempDir);
    this.gitContextMock.setTrackedRemote("upstream");
    FileAccess fileAccess = new FileAccessImpl(context);
    fileAccess.mkdirs(tempDir.resolve(GitContext.GIT_FOLDER));
    // act
    context.getGitContext().pullOrClone(GitUrl.of(gitRepoUrl), tempDir);
    // assert
    assertThat(tempDir.resolve(GitContext.GIT_FOLDER).resolve("update")).hasContent(this.processContext.getNow().toString());
  }

  /**
   * Runs a simulated git pull on a repository without any remote and checks that the user is asked whether to continue.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testRunGitPullWithoutRemoteAsksToContinue(@TempDir Path tempDir) {

    // arrange
    String gitRepoUrl = "https://github.com/test";
    IdeTestContext context = newGitContext(tempDir);
    this.gitContextMock.setTrackedRemote(null);
    FileAccess fileAccess = new FileAccessImpl(context);
    fileAccess.mkdirs(tempDir.resolve(GitContext.GIT_FOLDER));
    // act + assert (no answers are configured, so asking a question fails)
    assertThatThrownBy(() -> context.getGitContext().pullOrClone(GitUrl.of(gitRepoUrl), tempDir)).isInstanceOf(IllegalStateException.class);
  }

  /**
   * Runs a git pull with force mode, creates temporary files to simulate a proper cleanup.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testRunGitPullWithForceStartsReset(@TempDir Path tempDir) {

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
  void testRunGitPullWithForceStartsCleanup(@TempDir Path tempDir) {

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

  @Test
  void testGitRepoIsRecognizedCorrectly(@TempDir Path tempDir) {
    String gitRepoUrl = "https://github.com/test";

    IdeTestContext context = newGitContext(tempDir);
    GitContext gitContext = context.getGitContext();

    gitContext.pullOrCloneAndResetIfNeeded(GitUrl.ofMain(gitRepoUrl), tempDir, "origin");

    assertThat(gitContext.isGitRepo(tempDir)).isTrue();
  }

  @Test
  void testNormalDirIsNoRepo(@TempDir Path tempDir) {
    IdeTestContext context = newGitContext(tempDir);
    GitContext gitContext = context.getGitContext();

    FileAccess fileAccess = context.getFileAccess();
    fileAccess.mkdirs(tempDir.resolve("new-folder"));

    assertThat(gitContext.isGitRepo(tempDir)).isFalse();
  }

  /**
   * Test for fetchIfNeeded when the system is offline.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testFetchIfNeededOffline(@TempDir Path tempDir) {
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
  void testFetchIfNeededOnline(@TempDir Path tempDir) {
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
  void testFetchIfNeededRecentFetchHead(@TempDir Path tempDir) throws IOException {
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
   * Test for fetch when no remote is specified and the current branch has no upstream, which should fetch all remotes.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testFetchAllRemotesWhenNoRemoteAndNoUpstream(@TempDir Path tempDir) {
    // arrange
    IdeTestContext context = newGitContext(tempDir);
    this.gitContextMock.setTrackedRemote(null);
    GitContext gitContext = context.getGitContext();

    // act
    gitContext.fetch(tempDir, null, null);

    // assert
    assertThat(this.processContext.getResults()).hasSize(2);
    assertThat(this.processContext.getResults().getLast().getCommand()).isEqualTo("git fetch --all");
  }

  /**
   * Test for isRepositoryUpdateAvailable when local and remote commits are the same.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testIsRepositoryUpdateAvailableNoUpdates(@TempDir Path tempDir) {
    // arrange
    List<String> errors = new ArrayList<>();
    List<String> outs = new ArrayList<>();
    outs.add("local_commit_hash");
    outs.add("local_commit_hash"); // same as remote to simulate no updates
    IdeContext context = newGitContext(tempDir);

    // act
    boolean result = context.getGitContext().isRepositoryUpdateAvailable(tempDir);

    // assert
    assertThat(result).isFalse(); // No updates should be available
  }


  /**
   * Runs a simulated git rest.
   *
   * @param tempDir a {@link TempDir} {@link Path}.
   */
  @Test
  void testRunGitReset(@TempDir Path tempDir) {

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
