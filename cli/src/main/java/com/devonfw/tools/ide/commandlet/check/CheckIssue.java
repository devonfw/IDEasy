package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

public class CheckIssue {

  private final Path path;
  private final Integer line;
  private final String message;

  protected CheckIssue(Path path, String message) {

    this.path = path;
    this.line = null;
    this.message = message;
  }

  protected CheckIssue(Path path, Integer line, String message) {
    this.path = path;
    this.line = line;
    this.message = message;
  }

  public Path getPath() {

    return this.path;
  }

  public Integer getLineNumber() {

    return this.line;
  }

  public String getMessage() {

    return this.message;
  }

  public boolean isFixable() {
    return false;
  }

  public boolean fix(IdeContext context) {
    return false;
  }

  @Override
  public String toString() {
    if (this.line == null) {
      return this.path + ": " + this.message;
    }
    return this.path + ":" + this.line + ": " + this.message;
  }
}
