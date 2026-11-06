package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.devonfw.tools.ide.git.GitContextImpl;
import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

/**
 * Mocks the {@link ProcessContext}.
 */
public class ProcessContextGitMock extends ProcessContextImpl {

  private final LocalDateTime now;

  private final Path directory;

  private final List<OutputMessage> outputMessages;

  private final Deque<QueuedCommand> queuedCommands;

  private final List<ProcessResult> results;

  /**
   * A single git command that is queued to be returned for the next {@code git} invocation, either a successful command producing a {@link OutputMessage} or a
   * failed command with a non-zero exit code.
   *
   * @param exitCode the exit code to return for this command.
   * @param message the {@link OutputMessage} to return for this command.
   */
  private record QueuedCommand(int exitCode, OutputMessage message) {

  }

  /**
   * @param directory the {@link Path} to the git repository.
   */
  public ProcessContextGitMock(IdeContext context, Path directory) {

    super(context);
    this.directory = directory;
    this.now = LocalDateTime.now();
    this.outputMessages = new ArrayList<>();
    this.queuedCommands = new ArrayDeque<>();
    this.results = new ArrayList<>();
  }

  /**
   * @param message the {@link OutputMessage} to add.
   */
  public void addOutputMessage(OutputMessage message) {
    this.outputMessages.add(message);
  }

  /**
   * Queues a single-line {@link OutputMessage} to be returned as the out of the next {@code git} command that is run. This allows a sequence of git invocations
   * to each produce a distinct result, which is useful when a single flow issues multiple commands (for example
   * {@link GitContextImpl#isRepositoryUpdateAvailable(Path)}).
   *
   * @param message the single-line {@link OutputMessage} to return for the next git command.
   */
  public void addCommandOutput(OutputMessage message) {
    this.queuedCommands.addLast(new QueuedCommand(ProcessResult.SUCCESS, message));
  }

  /**
   * Queues a failed {@code git} command that is run next: it returns the given non-zero {@code exitCode} and an error {@link OutputMessage} (standard error),
   * so that the caller observes {@code isSuccessful() == false}.
   *
   * @param exitCode the non-zero exit code to return for the next git command.
   * @param errorMessage the standard error {@link OutputMessage} to return for the next git command.
   */
  public void addCommandFailure(int exitCode, OutputMessage errorMessage) {
    this.queuedCommands.addLast(new QueuedCommand(exitCode, errorMessage));
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

  @Override
  public ProcessContext createChild() {

    return this;
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

    if (!this.executable.getFileName().toString().equals("git")) {
      return super.run(processMode);
    }
    // consume the next queued git result so consecutive git invocations can return different outputs or failures
    QueuedCommand queuedCommand = this.queuedCommands.pollFirst();
    int exitCode = ProcessResult.SUCCESS;
    if (queuedCommand != null) {
      exitCode = queuedCommand.exitCode();
      if (queuedCommand.message() != null) {
        this.outputMessages.add(queuedCommand.message());
      }
    }
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
    if (this.arguments.contains("pull")) {
      try {
        Files.createDirectories(gitFolderPath);
        Path newFile = Files.createFile(gitFolderPath.resolve("update"));
        Files.writeString(newFile, this.now.toString());
        exitCode = ProcessResult.SUCCESS;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    this.arguments.clear();
    List<OutputMessage> outputMessagesCopy = List.copyOf(this.outputMessages);
    this.outputMessages.clear();
    ProcessResultImpl result = new ProcessResultImpl("git", command.toString(), exitCode, outputMessagesCopy);
    this.results.add(result);
    return result;
  }

}
