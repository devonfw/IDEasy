package com.devonfw.tools.ide.configurator;

import java.io.File;

import com.devonfw.tools.ide.logging.Log;

/**
 * Simple container for the commandline arguments.
 *
 * @since 3.0.0
 */
public class Args {

  private final String[] args;

  private int i;

  /**
   * The constructor.
   *
   * @param args the arguments.
   */
  public Args(String... args) {

    this.args = args;
    this.i = 0;
  }

  /**
   * @return {@code true} if {@link #next() next argument} is available, {@code false} otherwise.
   */
  public boolean hasNext() {

    return (this.i < this.args.length);
  }

  /**
   * @return the current argument.
   */
  public String current() {

    if (this.i < this.args.length) {
      return this.args[this.i];
    }
    return null;
  }

  /**
   * @return the next argument.
   * @see #hasNext()
   */
  public String next() {

    if (this.i < this.args.length) {
      return this.args[this.i++];
    }
    return null;
  }

  /**
   * @param file the current {@link File} variable to check if the parameter was already applied. Initially
   *        {@code null}.
   * @return the {@link #next() next argument} as {@link File}.
   */
  public File nextFile(File file) {

    String arg = current();
    if (file != null) {
      Log.warn("Duplicate option '" + arg + "'.");
    }
    File result = file;
    if (this.i < this.args.length) {
      result = new File(this.args[this.i++]);
    } else {
      Log.err("Option '" + arg + "' has to be followed by a file argument.");
    }
    return result;
  }

}
