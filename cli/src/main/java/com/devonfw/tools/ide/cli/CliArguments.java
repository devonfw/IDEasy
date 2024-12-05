package com.devonfw.tools.ide.cli;

import java.util.Iterator;

/**
 * Wraps {@link CliArgument} as state object allowing to consume arguments.
 */
public class CliArguments implements Iterator<CliArgument> {

  private final CliArgument initialArgument;

  private CliArgument currentArg;

  private boolean endOptions;

  private boolean splitShortOpts;

  /**
   * The constructor.
   *
   * @param args the {@link CliArgument#of(String...) command line arguments}.
   */
  public CliArguments(String... args) {

    this(CliArgument.of(args));
  }

  /**
   * The constructor.
   *
   * @param arg the {@link #current() initial} {@link CliArgument}.
   */
  public CliArguments(CliArgument arg) {

    this(arg, false, true);
  }

  CliArguments(CliArgument arg, boolean endOpts, boolean splitShortOpts) {

    super();
    this.initialArgument = arg;
    this.endOptions = endOpts;
    this.splitShortOpts = splitShortOpts;
    setCurrent(arg);
  }

  /**
   * Marks the end of the options so no further {@link CliArgument#getNext(boolean) option splitting} will be performed.
   *
   * @see #stopSplitShortOptions()
   */
  public void endOptions() {

    this.endOptions = true;
    this.splitShortOpts = false;
  }

  /**
   * Stops splitting of short options.
   *
   * @see CliArgument#getNext(boolean)
   * @see #endOptions()
   */
  public void stopSplitShortOptions() {

    this.splitShortOpts = false;
  }

  /**
   * @return {@code true} if short options (e.g. "-bdf") should not be split (e.g. into "-b -d -f" for "--batch --debug --force"), {@code false} otherwise.
   */
  public boolean isSplitShortOpts() {

    return splitShortOpts;
  }

  /**
   * @return {@code true} if the options have ended, {@code false} otherwise.
   * @see CliArgument#isEndOptions()
   * @see com.devonfw.tools.ide.property.Property#isEndOptions()
   * @see #endOptions()
   */
  public boolean isEndOptions() {

    return this.endOptions;
  }

  private void setCurrent(CliArgument arg) {

    if (arg.isEndOptions()) {
      endOptions();
      this.currentArg = arg.getNext();
    } else {
      this.currentArg = arg;
    }
  }

  /**
   * @return {@code true} if the last argument shall be {@link CliArgument#isCompletion() completed}, {@code false}.
   */
  public boolean isCompletion() {

    CliArgument arg = this.currentArg;
    while ((arg != null) && !arg.isEnd()) {
      if (arg.isCompletion()) {
        return true;
      }
      arg = arg.next;
    }
    return false;
  }

  /**
   * @return the initial {@link CliArgument}.
   */
  public CliArgument getInitialArgument() {

    return this.initialArgument;
  }

  /**
   * @return the current {@link CliArgument}.
   * @see #hasNext()
   * @see #next()
   */
  public CliArgument current() {

    return this.currentArg;
  }

  @Override
  public boolean hasNext() {

    if (this.currentArg.isEnd()) {
      return false;
    }
    return !this.currentArg.next.isEnd();
  }

  /**
   * Consumes the {@link #current() current argument} and proceeds to the next one.
   *
   * @return the next {@link CliArgument}.
   */
  @Override
  public CliArgument next() {

    if (!this.currentArg.isEnd()) {
      setCurrent(this.currentArg.getNext(this.splitShortOpts));
    }
    return this.currentArg;
  }

  /**
   * @return a copy of this {@link CliArguments} to fork a CLI matching of auto-completion.
   */
  public CliArguments copy() {

    return new CliArguments(this.currentArg, this.endOptions, this.splitShortOpts);
  }

  @Override
  public String toString() {

    return this.currentArg.getArgs();
  }

  /**
   * @param args the {@link CliArgument#of(String...) command line arguments}.
   * @return the {@link CliArguments}.
   */
  public static CliArguments ofCompletion(String... args) {

    return new CliArguments(CliArgument.ofCompletion(args));
  }

}
