package com.devonfw.tools.ide.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

  private final List<String> errors;

  private final List<String> outs;

  private final LocalDateTime now;

  private int exitCode;

  private final Path directory;

  /**
   * @param directory the {@link Path} to the git repository.
   */
  public ProcessContextGitMock(Path directory) {

    this.arguments = new ArrayList<>();
    this.errors = new ArrayList<>();
    this.outs = new ArrayList<>();
    this.exitCode = ProcessResult.SUCCESS;
    this.directory = directory;
    this.now = LocalDateTime.now();
  }

  /**
   * @return the mocked {@link ProcessResult#getExitCode() exit code}.
   */
  public int getExitCode() {

    return this.exitCode;
  }

  /**
   * @param exitCode the {@link #getExitCode() exit code}.
   */
  public void setExitCode(int exitCode) {

    this.exitCode = exitCode;
  }

  /**
   * @return the {@link List} of mocked error messages.
   */
  public List<String> getErrors() {

    return errors;
  }

  /**
   * @return the {@link List} of mocked out messages.
   */
  public List<String> getOuts() {

    return outs;
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
  public ProcessResult run(ProcessMode processMode) {

    Path gitFolderPath = this.directory.resolve(".git");
    // deletes a newly added folder
    if (this.arguments.contains("clean")) {
      try {
        Files.deleteIfExists(this.directory.resolve("new-folder"));
        this.exitCode = 0;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    // part of git cleanup checks if a new directory 'new-folder' exists
    if (this.arguments.contains("ls-files")) {
      if (Files.exists(this.directory.resolve("new-folder"))) {
        this.outs.add("new-folder");
        this.exitCode = 0;
      }
    }
    if (this.arguments.contains("clone")) {
      try {
        Files.createDirectories(gitFolderPath);
        Path newFile = Files.createFile(gitFolderPath.resolve("url"));
        // 3rd argument = repository Url
        Files.writeString(newFile, this.arguments.get(2));
        this.exitCode = 0;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    // always consider that files were changed
    if (this.arguments.contains("diff-index")) {
      this.exitCode = 1;
    }
    // changes file back to initial state (uses reference file in .git folder)
    if (this.arguments.contains("reset")) {
      try {
        if (Files.exists(gitFolderPath.resolve("objects").resolve("referenceFile"))) {
          Files.copy(gitFolderPath.resolve("objects").resolve("referenceFile"), this.directory.resolve("trackedFile"),
              StandardCopyOption.REPLACE_EXISTING);
        }
        this.exitCode = 0;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (this.arguments.contains("pull")) {
      try {
        Files.createDirectories(gitFolderPath);
        Path newFile = Files.createFile(gitFolderPath.resolve("update"));
        Files.writeString(newFile, this.now.toString());
        this.exitCode = 0;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    this.arguments.clear();
    return new ProcessResultImpl(this.exitCode, this.outs, this.errors);
  }

}
