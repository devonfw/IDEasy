package com.devonfw.tools.ide.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Mock implementation of {@link GitContext}.
 */
public class GitContextMock implements GitContext {

  private static final String MOCKED_URL_VALUE = "mocked url value";

  private final Map<Path, List<GitCommit>> pending = new HashMap<>();

  @Override
  public void pullOrCloneIfNeeded(GitUrl gitUrl, Path repository) {
    Path gitFolder = repository.resolve(GIT_FOLDER);
    if (Files.exists(gitFolder)) {
      pull(repository);
    } else {
      clone(gitUrl, repository);
    }
  }

  @Override
  public void pullOrCloneAndResetIfNeeded(GitUrl gitUrl, Path repository, String remoteName) {
    pullOrCloneIfNeeded(gitUrl, repository);
  }

  @Override
  public void pullSafelyWithStash(Path repository) {
    pull(repository);
  }

  @Override
  public boolean hasUntrackedFiles(Path repository) {
    return false;
  }

  @Override
  public void pullOrClone(GitUrl gitUrl, Path repository) {
    pullOrCloneIfNeeded(gitUrl, repository);
  }

  /**
   * Simulates cloning a remote repository into the provided local path for tests.
   *
   * @param gitUrl the {@link GitUrl} describing remote and branch
   * @param repository the target repository path
   */
  @Override
  public void clone(GitUrl gitUrl, Path repository) {
    try {
      Files.createDirectories(repository);
      Path gitFolder = repository.resolve(GIT_FOLDER);
      Files.createDirectories(gitFolder);
      // create HEAD pointing to branch
      String branch = gitUrl.branch();
      if (branch == null || branch.isEmpty()) {
        branch = GitUrl.BRANCH_MAIN;
      }
      String headContent = "ref: refs/heads/" + branch;
      Files.writeString(gitFolder.resolve(FILE_HEAD), headContent);
      // create FETCH_HEAD containing hashCode of GitUrl and also create a refs/heads entry
      String lastId = String.valueOf(gitUrl.hashCode());
      Files.writeString(gitFolder.resolve(FILE_FETCH_HEAD), lastId);
      Path refsHeads = gitFolder.resolve("refs").resolve("heads");
      Files.createDirectories(refsHeads);
      Path branchRefPath = refsHeads.resolve(branch);
      Files.createDirectories(branchRefPath.getParent());
      Files.writeString(branchRefPath, lastId);
      // create a simple config file with remote origin url using ini-like format
      StringBuilder cfg = new StringBuilder();
      cfg.append("[remote \"origin\"]\n");
      cfg.append("\turl = ").append(gitUrl.url()).append('\n');
      Files.writeString(gitFolder.resolve("config"), cfg.toString());
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Applies pending commits for the given repository.
   *
   * @param repository the repository to pull into
   */
  @Override
  public void pull(Path repository) {
    // apply all pending commits for this repository if any
    List<GitCommit> list = this.pending.get(repository);
    if (list == null || list.isEmpty()) {
      return;
    }
    // remember last pending commit id before applying
    GitCommit last = list.get(list.size() - 1);
    try {
      for (GitCommit commit : new ArrayList<>(list)) {
        for (GitChange change : commit.getChanges()) {
          Path src = change.content();
          Path dest = repository.resolve(change.target());
          // ensure parent dirs
          Files.createDirectories(dest.getParent());
          // copy file or directory
          if (Files.isDirectory(src)) {
            copyDirectory(src, dest);
          } else {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        }
        // remove applied commit
        list.remove(0);
      }
      // update refs/heads/<branch> and FETCH_HEAD to last applied commit id
      Path gitFolder = repository.resolve(GIT_FOLDER);
      if (!Files.exists(gitFolder)) {
        Files.createDirectories(gitFolder);
      }
      String branch = determineCurrentBranch(repository);
      String lastId = String.valueOf(last.hashCode());
      Path refsHeads = gitFolder.resolve("refs").resolve("heads");
      Files.createDirectories(refsHeads);
      Path branchRefPath = refsHeads.resolve(branch);
      Files.createDirectories(branchRefPath.getParent());
      Files.writeString(branchRefPath, lastId);
      Files.writeString(gitFolder.resolve(FILE_FETCH_HEAD), lastId);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Simulates fetching by updating {@code .git/FETCH_HEAD} to the id of the last pending commit.
   *
   * @param repository the repository to fetch into
   * @param remote the remote name (ignored in the mock)
   * @param branch the branch name (ignored in the mock)
   */
  @Override
  public void fetch(Path repository, String remote, String branch) {
    List<GitCommit> list = this.pending.get(repository);
    if (list == null || list.isEmpty()) {
      return;
    }
    GitCommit last = list.get(list.size() - 1);
    Path gitFolder = repository.resolve(GIT_FOLDER);
    try {
      Files.createDirectories(gitFolder);
      Files.writeString(gitFolder.resolve(FILE_FETCH_HEAD), String.valueOf(last.hashCode()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    Path cfg = repository.resolve(GIT_FOLDER).resolve("config");
    if (Files.exists(cfg)) {
      try {
        for (String line : Files.readAllLines(cfg)) {
          String trimmed = line.trim();
          if (trimmed.startsWith("url =")) {
            return trimmed.substring("url =".length()).trim();
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return MOCKED_URL_VALUE;
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
   * Performs fetch only when pending commits exist for the repository.
   *
   * @param repository the repository to potentially fetch
   * @param remoteName the remote name (ignored)
   * @param branch the branch name (ignored)
   * @return {@code true} if fetch was performed, {@code false} otherwise
   */
  @Override
  public boolean fetchIfNeeded(Path repository, String remoteName, String branch) {
    List<GitCommit> list = this.pending.get(repository);
    if (list == null || list.isEmpty()) {
      return false;
    }
    fetch(repository, remoteName, branch);
    return true;
  }

  @Override
  public boolean fetchIfNeeded(Path repository) {
    return fetchIfNeeded(repository, DEFAULT_REMOTE, null);
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
    Path fetch = gitFolder.resolve(FILE_FETCH_HEAD);
    Path head = gitFolder.resolve(FILE_HEAD);
    try {
      if (!Files.exists(fetch) || !Files.exists(head)) {
        return false;
      }
      String f = Files.readString(fetch).trim();
      String h = Files.readString(head).trim();
      if (h.startsWith("ref:")) {
        String ref = h.substring("ref:".length()).trim();
        Path refPath = gitFolder.resolve(ref);
        if (!Files.exists(refPath)) {
          // cannot resolve HEAD ref -> treat as no update
          return false;
        }
        h = Files.readString(refPath).trim();
      }
      return !f.equals(h);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    Path gitFolder = repository.resolve(GIT_FOLDER);
    Path fetch = gitFolder.resolve(FILE_FETCH_HEAD);
    try {
      if (!Files.exists(fetch) || !Files.exists(trackedCommitIdPath)) {
        return false;
      }
      String f = Files.readString(fetch).trim();
      String t = Files.readString(trackedCommitIdPath).trim();
      return !f.equals(t);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Determines the current branch from {@code .git/HEAD}. If HEAD contains a ref the branch name is extracted from the ref. If HEAD does not exist the default
   * branch {@link GitUrl#BRANCH_MAIN} is returned.
   *
   * @param repository the repository to inspect
   * @return the current branch name
   */
  @Override
  public String determineCurrentBranch(Path repository) {
    Path head = repository.resolve(GIT_FOLDER).resolve(FILE_HEAD);
    if (!Files.exists(head)) {
      return GitUrl.BRANCH_MAIN;
    }
    try {
      String content = Files.readString(head).trim();
      final String prefix = "ref: refs/heads/";
      if (content.startsWith(prefix)) {
        return content.substring(prefix.length());
      }
      // fallback: handle other ref formats
      if (content.startsWith("ref:")) {
        int idx = content.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < content.length()) {
          return content.substring(idx + 1);
        }
      }
      // fallback: return content
      return content;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    List<GitCommit> list = this.pending.computeIfAbsent(repository, k -> new ArrayList<>());
    if (commits != null) {
      Collections.addAll(list, commits);
    }
  }

  /**
   * Copies a directory tree from {@code source} to {@code target}, overwriting existing files.
   *
   * @param source the source directory
   * @param target the destination directory
   * @throws IOException on IO errors
   */
  private static void copyDirectory(Path source, Path target) throws IOException {
    try (Stream<Path> stream = Files.walk(source)) {
      stream.forEach(s -> {
        try {
          Path rel = source.relativize(s);
          Path dest = target.resolve(rel);
          if (Files.isDirectory(s)) {
            Files.createDirectories(dest);
          } else {
            Files.createDirectories(dest.getParent());
            Files.copy(s, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  /**
   * Represents a file or directory change to apply when pulling a commit.
   */
  public static class GitChange {

    private final Path content;
    private final Path target;

    public GitChange(Path content, Path target) {
      this.content = content;
      this.target = target;
    }

    public Path content() {
      return this.content;
    }

    public Path target() {
      return this.target;
    }
  }

  /**
   * Represents a commit containing one or more {@link GitChange}s.
   */
  public static class GitCommit {

    private final List<GitChange> changes = new ArrayList<>();

    public GitCommit(GitChange... changes) {
      if (changes != null) {
        for (GitChange c : changes) {
          this.changes.add(c);
        }
      }
    }

    public List<GitChange> getChanges() {
      return Collections.unmodifiableList(this.changes);
    }

    @Override
    public int hashCode() {
      return this.changes.hashCode();
    }
  }


  /**
   * Saves the current commit id of the repository into the given file. If HEAD is a ref (e.g. "ref: refs/heads/main") the referenced ref file
   * (.git/refs/heads/main) is read to obtain the commit id. Otherwise the HEAD content is used directly. The commit id is written to trackedCommitIdPath
   * (parent directories are created if necessary).
   *
   * @param repository the repository
   * @param trackedCommitIdPath the file to store the commit id
   */
  @Override
  public void saveCurrentCommitId(Path repository, Path trackedCommitIdPath) {
    Path gitFolder = repository.resolve(GIT_FOLDER);
    Path head = gitFolder.resolve(FILE_HEAD);
    try {
      String commitId = "";
      if (Files.exists(head)) {
        String content = Files.readString(head).trim();
        if (content.startsWith("ref:")) {
          String ref = content.substring("ref:".length()).trim();
          Path refPath = gitFolder.resolve(ref);
          if (Files.exists(refPath)) {
            commitId = Files.readString(refPath).trim();
          } else {
            // ref file does not exist so do not save anything
            return;
          }
        } else {
          commitId = content;
        }
      }
      if (commitId == null || commitId.isEmpty()) {
        // nothing to save
        return;
      }
      Path parent = trackedCommitIdPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(trackedCommitIdPath, commitId);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
