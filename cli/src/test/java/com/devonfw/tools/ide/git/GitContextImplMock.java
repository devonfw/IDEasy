package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;

/**
 * Mock implementation of {@link GitContextImpl}.
 */
public class GitContextImplMock extends GitContextImpl {

  private static final Logger LOG = LoggerFactory.getLogger(GitContextImplMock.class);
  private static final String COMMIT_ID = "commit-id";

  private final Path repositoryPath;
  private boolean stashCreationFailed = false;
  private boolean stashListFailed = false;
  private boolean stashPopFailed = false;
  private boolean simulateUntrackedFiles = false;
  private final List<String> stashList = new ArrayList<>();

  /**
   * @param context the {@link IdeContext context}.
   * @param repository Path to the repository.
   */
  public GitContextImplMock(IdeContext context, Path repository) {
    super(context);
    this.repositoryPath = repository;
  }

  @Override
  public void fetch(Path repository, String remote, String branch) {
    getContext().getFileAccess().touch(repository.resolve(".git/FETCH_HEAD"));
  }

  @Override
  public void pull(Path repository) {
    getContext().getFileAccess().touch(repository.resolve(".git/HEAD"));
  }

  @Override
  public void clone(GitUrl gitUrl, Path repository) {
    // Copy the mocked repository to the target repository
    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.copy(this.repositoryPath, repository, FileCopyMode.COPY_TREE_CONTENT);
    try {
      // Create .git/FETCH_HEAD and .git/HEAD files
      Path gitFolder = repository.resolve(GIT_FOLDER);
      Files.createDirectory(gitFolder);
      fileAccess.touch(gitFolder.resolve(FILE_FETCH_HEAD));
      fileAccess.writeFileContent(gitUrl.toString(), gitFolder.resolve(FILE_HEAD));
      fileAccess.writeFileContent("70b100e95a5f6c48ae70e8eea302c48ad4874bd4", gitFolder.resolve(COMMIT_ID));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String determineRemote(Path repository) {

    return DEFAULT_REMOTE;
  }

  @Override
  public String determineCurrentBranch(Path repository) {

    return GitUrl.BRANCH_MAIN;
  }

  @Override
  protected String determineCurrentCommitId(Path repository) {

    Path gitFolder = repository.resolve(GIT_FOLDER);
    return this.context.getFileAccess().readFileContent(gitFolder.resolve(FILE_HEAD));
  }

  @Override
  public void reset(Path repository, String branchName, String remoteName) {

    FileAccess fileAccess = this.context.getFileAccess();
    fileAccess.listChildren(repository, f -> Files.isRegularFile(f) && f.getFileName().toString().contains("modified"))
        .forEach(fileAccess::delete);
  }

  @Override
  public Path findGitRequired() {
    return Path.of("git");
  }

  /**
   * Configures whether the stash creation should fail.
   *
   * @param shouldFail true if stash creation should fail, false otherwise
   */
  public void setStashCreationFailed(boolean shouldFail) {
    this.stashCreationFailed = shouldFail;
  }

  /**
   * Configures whether the stash list operation should fail.
   *
   * @param shouldFail true if stash list should fail, false otherwise
   */
  public void setStashListFailed(boolean shouldFail) {
    this.stashListFailed = shouldFail;
  }

  /**
   * Configures whether the stash pop operation should fail.
   *
   * @param shouldFail true if stash pop should fail, false otherwise
   */
  public void setStashPopFailed(boolean shouldFail) {
    this.stashPopFailed = shouldFail;
  }

  /**
   * Configures whether the repository should simulate untracked files.
   *
   * @param hasUntrackedFiles true if untracked files should be simulated, false otherwise
   */
  public void setSimulateUntrackedFiles(boolean hasUntrackedFiles) {
    this.simulateUntrackedFiles = hasUntrackedFiles;
  }

  /**
   * Adds a stash entry to the mock stash list.
   *
   * @param stashRef the stash reference (e.g., "stash@{0}")
   * @param message the stash message
   */
  public void addStashEntry(String stashRef, String message) {
    this.stashList.add(stashRef + ": " + message);
  }

  /**
   * Clears the stash list.
   */
  public void clearStashList() {
    this.stashList.clear();
  }

  @Override
  public boolean hasUntrackedFiles(Path repository) {
    return this.simulateUntrackedFiles;
  }

  @Override
  public void pullSafelyWithStash(Path repository) {
    String token = "autostash:pull:" + java.util.UUID.randomUUID();
    LOG.debug("Untracked files found. Creating temporary stash with token '{}'", token);

    // Simulate stash push
    if (!this.stashCreationFailed) {
      LOG.debug("Stash push successful");
    } else {
      LOG.warn("Failed to create stash before pull on {}", repository);
    }

    // Simulate stash list
    if (!this.stashListFailed) {
      LOG.debug("Stash list successful");
    } else {
      LOG.warn("Failed to list stash after creating temporary stash on {}", repository);
    }

    // Find stash ref by message
    String stashRef = findStashRefByMessageInList(token);
    if (stashRef == null) {
      LOG.warn("Could not find created stash by token '{}'. Leaving stash untouched.", token);
    } else {
      LOG.debug("Created stash identified as '{}'", stashRef);
    }

    // Call pull
    pull(repository);

    // Simulate stash pop
    if (stashRef != null) {
      if (!this.stashPopFailed) {
        LOG.debug("Stash {} successfully popped after pull.", stashRef);
      } else {
        LOG.warn("Applying stash {} failed after successful pull on {}.", stashRef, repository);
      }
    } else {
      LOG.warn("Skipping stash pop because stashRef is unknown (token '{}'). Stash remains on the stack.", token);
    }
  }

  /**
   * Find stash reference by message in the mock stash list.
   *
   * @param needle the token to search for
   * @return the stash reference or null if not found
   */
  private String findStashRefByMessageInList(String needle) {
    if (this.stashList.isEmpty()) {
      return null;
    }
    for (String line : this.stashList) {
      if (line.contains(needle)) {
        int idx = line.indexOf(':');
        if (idx > 0) {
          return line.substring(0, idx).trim();
        }
      }
    }
    return null;
  }
}
