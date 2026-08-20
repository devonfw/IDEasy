package com.devonfw.tools.ide.commandlet.check;

import java.nio.file.Path;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Represents an issue found by {@code ide check}.
 */
public class CheckIssue {

  private final Path path;

  private final Integer line;

  private final String message;

  /**
   * The constructor for an issue not tied to a specific line.
   *
   * @param path the {@link Path} to the affected file. May point to a file that does not exist.
   * @param message a description of the problem.
   */
  protected CheckIssue(Path path, String message) {

    this.path = path;
    this.line = null;
    this.message = message;
  }

  /**
   * The constructor.
   *
   * @param path the {@link Path} to the affected file. May point to a file that does not exist.
   * @param line the line number within {@code path} that this issue refers to.
   * @param message a description of the problem.
   */
  protected CheckIssue(Path path, Integer line, String message) {
    this.path = path;
    this.line = line;
    this.message = message;
  }

  /**
   * @return the {@link Path} to the affected file.
   */
  public Path getPath() {

    return this.path;
  }

  /**
   * @return the line number this warning refers to, or {@code null} if not applicable.
   */
  public Integer getLineNumber() {

    return this.line;
  }

  /**
   * @return a description of the problem.
   */
  public String getMessage() {

    return this.message;
  }

  /**
   * @return {@code true} if this warning can be automatically fixed via {@link #fix(IdeContext)}, {@code false} otherwise.
   */
  public boolean isFixable() {
    return false;
  }

  /**
   * Attempts to fix the problem that caused this warning.
   *
   * @param context the {@link IdeContext}.
   * @return {@code true} if the fix was applied successfully, {@code false} otherwise.
   */
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
