package com.devonfw.tools.ide.environment;

import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Container that represents a line from a properties (ide.properties) file. We do not use {@link java.util.Properties}
 * as we need support for exported variables, lists/arrays, and saving changes without loosing comments, etc.
 */
public abstract class VariableLine {

  /**
   * @return {@code true} if the variable is exported (e.g. "export MAVEN_OPTS=-Xmx20248m"), {@code false} otherwise.
   */
  public boolean isExport() {

    return false;
  }

  /**
   * @return the name of the variable. Will be {@code null} if not a regular variable line (e.g. a comment or empty
   *         line).
   */
  public String getName() {

    return null;
  }

  /**
   * @return the value of the variable. Will be {@code null} if not a regular variable line (e.g. a comment or empty
   *         line).
   */
  public String getValue() {

    return null;
  }

  /**
   * @return the comment line (including the '#' character). Will be {@code null} if not a comment line.
   */
  public String getComment() {

    return null;
  }

  /**
   * @param newName the new variable {@link #getName() name}.
   * @return the new {@link VariableLine} with the modified {@link #getName() name}.
   */
  public VariableLine withName(String newName) {

    throw new UnsupportedOperationException();
  }

  /**
   * @param newValue the new variable {@link #getValue() value}.
   * @return the new {@link VariableLine} with the modified {@link #getValue() value}.
   */
  public VariableLine withValue(String newValue) {

    throw new UnsupportedOperationException();
  }

  /**
   * @param newExport the new {@link #isExport() export} flag.
   * @return the new {@link VariableLine} with the modified {@link #isExport() export} flag.
   */
  public VariableLine withExport(boolean newExport) {

    throw new UnsupportedOperationException();
  }

  static final class Variable extends VariableLine {

    private boolean export;

    private final String name;

    private final String value;

    private final String line;

    Variable(boolean export, String name, String value) {

      this(export, name, value, null);
    }

    private Variable(boolean export, String name, String value, String line) {

      super();
      this.export = export;
      this.name = name;
      this.value = value;
      if (line == null) {
        StringBuilder sb = new StringBuilder();
        if (export) {
          sb.append("export ");
        }
        sb.append(this.name);
        sb.append('=');
        sb.append(this.value);
        this.line = sb.toString();
      } else {
        this.line = line;
      }
    }

    @Override
    public boolean isExport() {

      return this.export;
    }

    @Override
    public String getName() {

      return this.name;
    }

    @Override
    public String getValue() {

      return this.value;
    }

    @Override
    public VariableLine withName(String newName) {

      if (newName.equals(this.name)) {
        return this;
      }
      return new Variable(this.export, newName, this.value);
    }

    @Override
    public VariableLine withValue(String newValue) {

      if (newValue.equals(this.value)) {
        return this;
      }
      return new Variable(this.export, this.name, newValue);
    }

    @Override
    public VariableLine withExport(boolean newExport) {

      if (newExport == this.export) {
        return this;
      }
      return new Variable(newExport, this.name, this.value);
    }

    @Override
    public String toString() {

      return this.line;
    }

  }

  static final class Comment extends VariableLine {

    private final String line;

    private Comment(String line) {

      super();
      this.line = line;
    }

    @Override
    public String getComment() {

      return this.line;
    }

    @Override
    public String toString() {

      return this.line;
    }

  }

  static final class Garbage extends VariableLine {

    private final String line;

    Garbage(String line) {

      super();
      this.line = line;
    }

    @Override
    public String toString() {

      return this.line;
    }

  }

  static final class Empty extends VariableLine {

    private final String line;

    Empty(String line) {

      super();
      this.line = line;
    }

    @Override
    public String toString() {

      return this.line;
    }

  }

  static VariableLine of(String line, IdeLogger logger, Object source) {

    int len = line.length();
    int start = 0;
    while (start < len) {
      char c = line.charAt(start);
      if (c == ' ') {
        start++; // ignore leading spaces
      } else if (c == '#') {
        return new Comment(line);
      } else {
        break;
      }
    }
    if (start >= len) {
      return new Empty(line);
    }
    boolean export = false;
    int end = start;
    int space = -1;
    while (end < len) {
      char c = line.charAt(end);
      if (c == ' ') {
        if (space == -1) {
          space = end;
        }
      } else if (c == '=') {
        // .0123456789
        // "export ...="
        String name;
        if ((space == start + 6) && (end > start + 7) && line.substring(start, space).equals("export")) {
          name = line.substring(space + 1, end).trim();
          if (name.isEmpty()) {
            name = line.substring(start, end).trim();
          } else {
            export = true;
          }
        } else {
          name = line.substring(start, end).trim();
        }
        String value = line.substring(end + 1).trim();
        if (value.isEmpty()) {
          value = null;
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1);
        }
        return new Variable(export, name, value, line);
      }
      end++;
    }
    logger.warning("Ignoring corrupted line '{}' in {}", line, source);
    return new Garbage(line);
  }

  static VariableLine of(boolean export, String name, String value) {

    return new Variable(export, name, value);
  }

}
