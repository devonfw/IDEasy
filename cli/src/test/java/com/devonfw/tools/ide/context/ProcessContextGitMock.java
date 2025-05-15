package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.devonfw.tools.ide.process.OutputMessage;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.process.ProcessResultImpl;

/**
 * Mocks the {@link ProcessContext}.
 */
public class ProcessContextGitMock implements ProcessContext {

  private final List<String> arguments;

  private final LocalDateTime now;

  private final Path directory;

  private List<OutputMessage> outputMessages;

  /**
   * @param directory the {@link Path} to the git repository.
   */
  public ProcessContextGitMock(Path directory) {

    this.arguments = new ArrayList<>();
    this.directory = directory;
    this.now = LocalDateTime.now();
    this.outputMessages = new ArrayList<OutputMessage>();
  }

  public void addOutputMessage(OutputMessage message) {
    this.outputMessages.add(message);
  }

  @Override
  public ProcessContext errorHandling(ProcessErrorHandling handling) {

    return this;
  }

  @Override
  public ProcessContext directory(Path newDirectory) {

    return this;
  }

  @Override
  public ProcessContext executable(Path executable) {

    return this;
  }

  @Override
  public ProcessContext executable(Path executable, boolean check) {

    return this;
  }

  @Override
  public ProcessContext addArg(String arg) {

    this.arguments.add(arg);
    return this;
  }

  public LocalDateTime getNow() {

    return this.now;
  }

  @Override
  public ProcessContext withEnvVar(String key, String value) {

    return this;
  }

  @Override
  public ProcessContext withPathEntry(Path path) {

    return this;
  }

  @Override
  public ProcessResult run(ProcessMode processMode) {

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
    return new ProcessResultImpl("git", command.toString(), exitCode, outputMessagesCopy);
  }

}
