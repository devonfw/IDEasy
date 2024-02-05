package com.devonfw.tools.ide.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessResult;

/**
 * Mocks the {@link ProcessContext}.
 */
public class GitUtilsProcessContextMock implements ProcessContext {

  private final List<String> arguments;

  private final List<String> errors;

  private final List<String> outs;

  private final int exitCode;

  private final Path directory;

  /**
   * @param errors List of errors.
   * @param outs List of out texts.
   * @param exitCode the exit code.
   * @param directory
   */
  public GitUtilsProcessContextMock(List<String> errors, List<String> outs, int exitCode, Path directory) {

    this.arguments = new ArrayList<>();
    this.errors = errors;
    this.outs = outs;
    this.exitCode = exitCode;
    this.directory = directory;
  }

  @Override
  public ProcessContext errorHandling(ProcessErrorHandling handling) {

    return this;
  }

  @Override
  public ProcessContext directory(Path directory) {

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

  @Override
  public ProcessContext withEnvVar(String key, String value) {

    return this;
  }

  @Override
  public ProcessResult run(boolean capture) {

    Path gitFolderPath = this.directory.resolve(".git").resolve("status");
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
        outs.add("new-folder");
      }
    }
    if (this.arguments.contains("clone")) {
      try {
        Files.createDirectories(gitFolderPath);
        Path newFile = Files.createFile(gitFolderPath.resolve("url"));
        // 3rd argument = repository Url
        Files.writeString(newFile, arguments.get(2));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    if (this.arguments.contains("reset")) {

    }
    if (this.arguments.contains("pull")) {
      try {
        Files.createDirectories(gitFolderPath);
        Path newFile = Files.createFile(gitFolderPath.resolve("update"));
        Date currentDate = new Date();
        Files.writeString(newFile, currentDate.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    this.arguments.clear();
    return new GitUtilsProcessResultMock(errors, outs, exitCode);
  }

}
