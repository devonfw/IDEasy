package com.devonfw.tools.ide.git;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.io.ini.IniFile;
import com.devonfw.tools.ide.io.ini.IniFileImpl;
import com.devonfw.tools.ide.io.ini.IniSection;

/**
 * Mock implementation of {@link GitContext}.
 */
public class GitContextMock extends GitContextImpl {

  private static final String MOCKED_URL_VALUE = "mocked url value";

  private final Map<Path, List<GitCommit>> pending = new HashMap<>();

  /**
   * @param context the {@link IdeContext context}.
   */
  public GitContextMock(IdeContext context) {

    super(context);
  }

  @Override
  public void pullSafelyWithStash(Path repository) {
    pull(repository);
  }

  @Override
  public boolean hasUntrackedFiles(Path repository) {
    return false;
  }

  /**
   * Simulates cloning a remote repository into the provided local path for tests.
   *
   * @param gitUrl the {@link GitUrl} describing remote and branch
   * @param repository the target repository path
   */
  @Override
  public void clone(GitUrl gitUrl, Path repository) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.mkdirs(repository);

    Path gitFolder = repository.resolve(GIT_FOLDER);
    fileAccess.mkdirs(gitFolder);
    String branch = gitUrl.branch();
    if ((branch == null) || branch.isEmpty()) {
      branch = GitUrl.BRANCH_MAIN;
    }

    String headContent = "ref: refs/heads/" + branch;
    String lastId = String.valueOf(gitUrl.hashCode());

    fileAccess.writeFileContent(headContent, gitFolder.resolve(FILE_HEAD));
    fileAccess.writeFileContent(lastId, gitFolder.resolve(FILE_FETCH_HEAD));

    Path branchRefPath = gitFolder.resolve("refs").resolve("heads").resolve(branch);
    fileAccess.mkdirs(branchRefPath.getParent());
    fileAccess.writeFileContent(lastId, branchRefPath);

    IniFile config = new IniFileImpl();
    IniSection originSection = config.getOrCreateSection("remote \"origin\"");
    originSection.setProperty("url", gitUrl.url());
    fileAccess.writeIniFile(config, gitFolder.resolve("config"));
  }

  /**
   * Applies pending commits for the given repository.
   *
   * @param repository the repository to pull into
   */
  @Override
  public void pull(Path repository) {
    List<GitCommit> commits = this.pending.get(repository);
    if ((commits == null) || commits.isEmpty()) {
      return;
    }
    FileAccess fileAccess = this.context.getFileAccess();
    GitCommit lastCommit = commits.getLast();

    for (GitCommit commit : commits) {
      for (GitChange change : commit.changes()) {
        Path source = change.content();
        Path target = repository.resolve(change.target());

        FileCopyMode copyMode;
        if (fileAccess.isFile(source)) {
          copyMode = FileCopyMode.COPY_FILE_TO_TARGET_OVERRIDE;
        } else {
          copyMode = FileCopyMode.COPY_TREE_CONTENT;
        }

        fileAccess.copy(source, target, copyMode);
      }
    }
    Path gitFolder = repository.resolve(GIT_FOLDER);
    fileAccess.mkdirs(gitFolder);

    String branch = determineCurrentBranch(repository);
    String lastCommitId = String.valueOf(lastCommit.hashCode());

    Path branchRefPath = gitFolder.resolve("refs").resolve("heads").resolve(branch);
    fileAccess.mkdirs(branchRefPath.getParent());
    fileAccess.writeFileContent(lastCommitId, branchRefPath);
    fileAccess.writeFileContent(lastCommitId, gitFolder.resolve(FILE_FETCH_HEAD));

    this.pending.remove(repository);
  }

  /**
   * Simulates fetching by updating {@code .git/FETCH_HEAD} to the ID of the latest pending commit.
   *
   * @param repository the repository to fetch into
   * @param remote the remote name (ignored in the mock)
   * @param branch the branch name (ignored in the mock)
   */
  @Override
  public void fetch(Path repository, String remote, String branch) {
    List<GitCommit> commits = this.pending.get(repository);
    if ((commits == null) || commits.isEmpty()) {
      return;
    }
    GitCommit lastCommit = commits.getLast();
    Path fetchHead = repository.resolve(GIT_FOLDER).resolve(FILE_FETCH_HEAD);

    this.context.getFileAccess().writeFileContent(String.valueOf(lastCommit.hashCode()), fetchHead);
  }

  @Override
  public void reset(Path repository, String branchName, String remoteName) {

  }

  @Override
  public void cleanup(Path repository) {

  }

  /**
   * Reads the remote URL from {@code .git/config}.
   *
   * @param repository the repository to inspect
   * @return the remote URL or a mocked constant if no config exists
   */
  @Override
  public String retrieveGitUrl(Path repository) {
    Path configPath = repository.resolve(GIT_FOLDER).resolve("config");
    FileAccess fileAccess = this.context.getFileAccess();

    if (!fileAccess.isFile(configPath)) {
      return MOCKED_URL_VALUE;
    }

    IniFile config = fileAccess.readIniFile(configPath);
    IniSection origin = config.getSection("remote \"origin\"");
    if (origin != null) {
      String url = origin.getPropertyValue("url");
      if ((url != null) && !url.isBlank()) {
        return url.trim();
      }
    }
    return MOCKED_URL_VALUE;
  }

  @Override
  public List<String> retrieveGitRemotes(Path repository) {

    return Collections.emptyList();
  }

  @Override
  public Path findGitRequired() {
    return Path.of("git");
  }

  @Override
  public Path findGit() {
    return null;
  }

  /**
   * Checks whether a mocked remote update is available by comparing {@code .git/FETCH_HEAD} with the current HEAD (resolving refs when necessary).
   *
   * @param repository the repository to check
   * @return {@code true} if an update is available
   */
  @Override
  public boolean isRepositoryUpdateAvailable(Path repository) {
    Path gitFolder = repository.resolve(GIT_FOLDER);
    FileAccess fileAccess = this.context.getFileAccess();

    String fetchCommitId = fileAccess.readFileContent(gitFolder.resolve(FILE_FETCH_HEAD));
    String currentCommitId = determineCurrentCommitId(repository);

    if ((fetchCommitId == null) || (currentCommitId == null)) {
      return false;
    }
    return !fetchCommitId.trim().equals(currentCommitId.trim());
  }

  /**
   * Checks whether a mocked remote update is available by comparing {@code .git/FETCH_HEAD} with a tracked commit id file.
   *
   * @param repository the repository to check
   * @param trackedCommitIdPath path to the tracked commit id file
   * @return {@code true} if an update is available
   */
  @Override
  public boolean isRepositoryUpdateAvailable(Path repository, Path trackedCommitIdPath) {
    FileAccess fileAccess = this.context.getFileAccess();
    String fetchCommitId = fileAccess.readFileContent(repository.resolve(GIT_FOLDER).resolve(FILE_FETCH_HEAD));
    String trackedCommitId = fileAccess.readFileContent(trackedCommitIdPath);

    if ((fetchCommitId == null) || (trackedCommitId == null)) {
      return false;
    }
    return !fetchCommitId.trim().equals(trackedCommitId.trim());
  }

  /**
   * Determines the current branch from {@code .git/HEAD}.
   *
   * @param repository the repository to inspect.
   * @return the current branch name.
   */
  @Override
  public String determineCurrentBranch(Path repository) {
    Path head = repository.resolve(GIT_FOLDER).resolve(FILE_HEAD);
    String content = this.context.getFileAccess().readFileContent(head);
    if ((content == null) || content.isBlank()) {
      return GitUrl.BRANCH_MAIN;
    }
    content = content.trim();
    final String prefix = "ref: refs/heads/";
    if (content.startsWith(prefix)) {
      return content.substring(prefix.length());
    }

    return content;
  }

  @Override
  public String determineRemote(Path repository) {
    return DEFAULT_REMOTE;
  }

  /**
   * Adds pending commits to simulate remote changes. Commits are stored per repository and applied on pull.
   *
   * @param repository the repository the commits belong to
   * @param commits the commits to add
   */
  public void addChanges(Path repository, GitCommit... commits) {
    if (commits == null) {
      return;
    }
    List<GitCommit> pendingCommits = this.pending.computeIfAbsent(repository, k -> new ArrayList<>());
    Collections.addAll(pendingCommits, commits);
  }

  @Override
  protected String determineCurrentCommitId(Path repository) {

    Path gitFolder = repository.resolve(GIT_FOLDER);
    FileAccess fileAccess = this.context.getFileAccess();

    String headContent = fileAccess.readFileContent(gitFolder.resolve(FILE_HEAD));

    if ((headContent == null) || headContent.isBlank()) {
      return null;
    }

    headContent = headContent.trim();

    if (!headContent.startsWith("ref:")) {
      return headContent;
    }

    final String ref = headContent.substring("ref:".length()).trim();
    String commitId = fileAccess.readFileContent(gitFolder.resolve(ref));

    if (commitId == null) {
      return null;
    }
    return commitId.trim();
  }

  /**
   * Represents a file or directory change to apply when pulling a commit.
   *
   * @param content path to the file or directory containing the change.
   * @param target relative target path inside the repository.
   */
  public record GitChange(Path content, Path target) {

  }

  /**
   * Represents a commit containing one or more {@link GitChange}s.
   *
   * @param changes the immutable changes contained in the commit.
   */
  public record GitCommit(List<GitChange> changes) {


    /**
     * Creates a new {@link GitCommit} with an immutable copy of the given changes.
     *
     * @param changes the changes contained in the commit.
     */
    public GitCommit {

      changes = List.copyOf(changes);
    }

    /**
     * Creates a new {@link GitCommit} from the given changes.
     *
     * @param changes the changes contained in the commit.
     */
    public GitCommit(GitChange... changes) {

      this(List.of(changes));
    }
  }

  @Override
  public void commit(Path repository, String message, boolean addAll) {

  }

  @Override
  public void tag(Path repository, String tagName, String message) {

  }

  @Override
  public void push(Path repository, boolean followTags) {

  }
}
