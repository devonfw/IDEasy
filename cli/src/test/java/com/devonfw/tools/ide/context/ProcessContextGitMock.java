package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

/**
 * Mocks the {@link ProcessContext}
 */
public class ProcessContextGitMock extends ProcessContextImpl {

  private static final String UNTRACKED_FILE = "untracked";
  private static final String STASH_LIST_FILE = "stash-list";
  private static final String STASH_CONTENTS_FOLDER = "stash-contents";
  private static final String STASH_TOP_REF = "stash@{0}";

  private final LocalDateTime now;

  private final Path directory;

  private final List<OutputMessage> outputMessages;

  private final List<ProcessResult> results;

  private boolean stashPushFailed;
  private boolean stashListFailed;
  private boolean stashPopFailed;
  private boolean pullFailed;

  /**
   * @param directory the {@link Path} to the git repository.
   */
  public ProcessContextGitMock(IdeContext context, Path directory) {

    super(context);
    this.directory = directory;
    this.now = LocalDateTime.now();
    this.outputMessages = new ArrayList<>();
    this.results = new ArrayList<>();
  }

  /**
   * @param message the {@link OutputMessage} to add.
   */
  public void addOutputMessage(OutputMessage message) {
    this.outputMessages.add(message);
  }

  /**
   * @return the {@link List} of collected {@link ProcessResult}s.
   */
  public List<ProcessResult> getResults() {

    return this.results;
  }

  public LocalDateTime getNow() {

    return this.now;
  }

  /**
   * Records an untracked file so it shows up in the simulated {@code git status} output.
   *
   * @param name the name of the untracked file
   */
  public void addUntrackedFile(String name) {

    writeLine(untrackedFile(), "?? " + name);
  }

  /**
   * Seeds the simulated stash list with a pre-existing entry (as reported by {@code git stash list}).
   *
   * @param stashRef the stash reference (e.g. {@code stash@{1}})
   * @param message the stash message
   */
  public void addStashEntry(String stashRef, String message) {

    writeLine(stashListFile(), stashRef + ": " + message);
  }

  /**
   * Configures the simulated {@code git stash push} to fail.
   *
   * @param failed {@code true} to simulate a failure
   */
  public void setStashPushFailed(boolean failed) {

    this.stashPushFailed = failed;
  }

  /**
   * Configures the simulated {@code git stash list} to fail.
   *
   * @param failed {@code true} to simulate a failure
   */
  public void setStashListFailed(boolean failed) {

    this.stashListFailed = failed;
  }

  /**
   * Configures the simulated {@code git stash pop} to fail.
   *
   * @param failed {@code true} to simulate a failure
   */
  public void setStashPopFailed(boolean failed) {

    this.stashPopFailed = failed;
  }

  /**
   * Configures the simulated {@code git pull} to fail.
   *
   * @param failed {@code true} to simulate a failure
   */
  public void setPullFailed(boolean failed) {

    this.pullFailed = failed;
  }

  @Override
  public ProcessContext createChild() {

    return this;
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

    if (!this.executable.getFileName().toString().equals("git")) {
      return super.run(processMode);
    }
    int exitCode = ProcessResult.SUCCESS;
    StringBuilder command = new StringBuilder("git");
    for (String arg : this.arguments) {
      command.append(' ');
      command.append(arg);
    }
    Path gitFolderPath = this.directory.resolve(".git");
    // deletes a newly added folder
    if (this.arguments.contains("clean")) {
      try {
        Files.deleteIfExists(this.directory.resolve("new-folder"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    // part of git cleanup checks if a new directory 'new-folder' exists
    if (this.arguments.contains("ls-files")) {
      if (Files.exists(this.directory.resolve("new-folder"))) {
        OutputMessage outputMessage = new OutputMessage(false, "new-folder");
        this.outputMessages.add(outputMessage);
      }
    }
    if (this.arguments.contains("clone")) {
      try {
        Files.createDirectories(gitFolderPath);
        Path newFile = Files.createFile(gitFolderPath.resolve("url"));
        // 3rd argument = repository Url
        Files.writeString(newFile, this.arguments.get(2));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    // always consider that files were changed
    if (this.arguments.contains("diff-index")) {
      exitCode = 1;
    }
    // changes file back to initial state (uses reference file in .git folder)
    if (this.arguments.contains("reset")) {
      try {
        if (Files.exists(gitFolderPath.resolve("objects").resolve("referenceFile"))) {
          Files.copy(gitFolderPath.resolve("objects").resolve("referenceFile"), this.directory.resolve("trackedFile"),
              StandardCopyOption.REPLACE_EXISTING);
        }
        exitCode = ProcessResult.SUCCESS;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    // simulates the stash push/list/pop lifecycle
    if (this.arguments.contains("stash")) {
      if (this.arguments.contains("push")) {
        exitCode = handleStashPush(argumentAfter("-m"));
      } else if (this.arguments.contains("pop")) {
        exitCode = handleStashPop(argumentAfter("pop"));
      } else {
        exitCode = handleStashList();
      }
    }
    // simulates "git status --porcelain" reporting the untracked files
    if (this.arguments.contains("status")) {
      exitCode = handleStatus();
    }
    // simulates "git branch --show-current" reporting the current branch (read from .git/HEAD)
    if (this.arguments.contains("branch")) {
      exitCode = handleBranch();
    }
    if (this.arguments.contains("pull")) {
      if (this.pullFailed) {
        exitCode = 1;
      } else {
        try {
          Files.createDirectories(gitFolderPath);
          Path newFile = Files.createFile(gitFolderPath.resolve("update"));
          Files.writeString(newFile, this.now.toString());
          exitCode = ProcessResult.SUCCESS;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    this.arguments.clear();
    List<OutputMessage> outputMessagesCopy = List.copyOf(this.outputMessages);
    this.outputMessages.clear();
    ProcessResultImpl result = new ProcessResultImpl("git", command.toString(), exitCode, outputMessagesCopy);
    this.results.add(result);
    return result;
  }

  /**
   * Simulates {@code git stash push}: the untracked files are moved into the stash contents and a new {@code stash@{0}} entry is prepended to the stash list,
   * pushing the existing entries down by one index (as real git does).
   *
   * @param token the stash message (value of the {@code -m} argument)
   * @return the exit code
   */
  private int handleStashPush(String token) {

    if (this.stashPushFailed) {
      return 1;
    }
    try {
      Path untracked = untrackedFile();
      Path contents = stashContentsFile(STASH_TOP_REF);
      if (Files.exists(untracked)) {
        Files.createDirectories(contents.getParent());
        Files.move(untracked, contents, StandardCopyOption.REPLACE_EXISTING);
      }
      prependStashEntry(STASH_TOP_REF, token);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return ProcessResult.SUCCESS;
  }

  /**
   * Simulates {@code git stash list} by reporting the lines of the stash list file.
   *
   * @return the exit code
   */
  private int handleStashList() {

    if (this.stashListFailed) {
      return 1;
    }
    for (String line : readLines(stashListFile())) {
      this.outputMessages.add(new OutputMessage(false, line));
    }
    return ProcessResult.SUCCESS;
  }

  /**
   * Simulates {@code git stash pop}: the given stash entry is removed from the stash list and its secured untracked files are restored.
   *
   * @param ref the stash reference to pop
   * @return the exit code
   */
  private int handleStashPop(String ref) {

    if (this.stashPopFailed) {
      return 1;
    }
    try {
      removeStashEntry(ref);
      Path contents = stashContentsFile(ref);
      if (Files.exists(contents)) {
        Files.move(contents, untrackedFile(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return ProcessResult.SUCCESS;
  }

  /**
   * Simulates {@code git status --porcelain} by reporting the untracked files.
   *
   * @return the exit code
   */
  private int handleStatus() {

    for (String line : readLines(untrackedFile())) {
      this.outputMessages.add(new OutputMessage(false, line));
    }
    return ProcessResult.SUCCESS;
  }

  /**
   * Simulates {@code git branch --show-current} by reporting the branch name read from the {@code .git/HEAD} file.
   *
   * @return the exit code
   */
  private int handleBranch() {

    String head = readSingleLine(gitDir().resolve("HEAD"));
    if (head.startsWith("ref: refs/heads/")) {
      this.outputMessages.add(new OutputMessage(false, head.substring("ref: refs/heads/".length())));
    }
    return ProcessResult.SUCCESS;
  }

  /**
   * Returns the argument following the given token, or an empty string if there is none.
   *
   * @param token the token to look for (e.g. {@code -m} or a sub-command)
   * @return the argument following the token
   */
  private String argumentAfter(String token) {

    int idx = this.arguments.indexOf(token);
    if ((idx >= 0) && (idx + 1 < this.arguments.size())) {
      return this.arguments.get(idx + 1);
    }
    return "";
  }

  private Path gitDir() {

    return this.directory.resolve(".git");
  }

  private Path untrackedFile() {

    return gitDir().resolve(UNTRACKED_FILE);
  }

  private Path stashListFile() {

    return gitDir().resolve(STASH_LIST_FILE);
  }

  private Path stashContentsFile(String ref) {

    return gitDir().resolve(STASH_CONTENTS_FOLDER).resolve(ref);
  }

  private static List<String> readLines(Path file) {

    try {
      if (Files.exists(file)) {
        return new ArrayList<>(Files.readAllLines(file));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new ArrayList<>();
  }

  private static String readSingleLine(Path file) {

    List<String> lines = readLines(file);
    if (lines.isEmpty()) {
      return "";
    }
    return lines.getFirst().trim();
  }

  private static void writeLine(Path file, String line) {

    try {
      Files.createDirectories(file.getParent());
      Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void prependStashEntry(String ref, String message) {

    Path list = stashListFile();
    List<String> lines = new ArrayList<>();
    lines.add(ref + ": " + message);
    for (String line : readLines(list)) {
      lines.add(bumpStashIndex(line));
    }
    try {
      Files.writeString(list, String.join(System.lineSeparator(), lines) + System.lineSeparator());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void removeStashEntry(String ref) {

    Path list = stashListFile();
    List<String> lines = readLines(list);
    lines.removeIf(line -> line.startsWith(ref + ":"));
    try {
      if (lines.isEmpty()) {
        Files.deleteIfExists(list);
      } else {
        Files.writeString(list, String.join(System.lineSeparator(), lines) + System.lineSeparator());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String bumpStashIndex(String line) {

    int open = line.indexOf('{');
    int close = line.indexOf('}');
    if ((open >= 0) && (close > open)) {
      int index = Integer.parseInt(line.substring(open + 1, close));
      return line.substring(0, open + 1) + (index + 1) + line.substring(close);
    }
    return line;
  }

}
