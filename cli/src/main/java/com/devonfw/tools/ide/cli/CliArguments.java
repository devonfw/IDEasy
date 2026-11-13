package com.devonfw.tools.ide.cli;

import java.util.Iterator;

/**
 * Wraps {@link CliArgument} as state object allowing to consume arguments.
 */
public class CliArguments implements Iterator<CliArgument> {

  private final CliArgument initialArgument;

  private CliArgument currentArg;

  private OptionPhase phase;

  /** If true, do not discard the special end-of-options token "--" but keep it as current argument. */
  private boolean preserveEndOptionsToken;

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

    this(arg, OptionPhase.OPTIONS);
  }

  CliArguments(CliArgument arg, OptionPhase phase) {

    this(arg, phase, false);
  }

  CliArguments(CliArgument arg, OptionPhase phase, boolean preserveEndOptionsToken) {

    super();
    this.initialArgument = arg;
    this.phase = phase;
    this.preserveEndOptionsToken = preserveEndOptionsToken;
    setCurrent(arg);
  }

  /**
   * Marks the end of the options: no further option splitting, but declared options are still matched ({@link OptionPhase#PASSTHROUGH}).
   */
  public void endOptions() {

    this.phase = OptionPhase.PASSTHROUGH;
  }

  /**
   * Marks the end of the options caused by a literal {@link CliArgument#isEndOptions() "--"} token: no further option matching and no option splitting.
   */
  public void endOptionsByToken() {

    this.phase = OptionPhase.AFTER_TOKEN;
  }

  /**
   * @return {@code true} if the options have ended, {@code false} otherwise.
   * @see OptionPhase#isOptionsEnded()
   */
  public boolean isEndOptions() {

    return this.phase.isOptionsEnded();
  }

  /** @return {@code true} if a declared option may still be matched (the options were not ended by a literal {@code "--"}). */
  public boolean matchesDeclaredOptions() {

    return this.phase.matchesDeclaredOptions();
  }

  /** @return {@code true} if combined short options (e.g. "-bdf") are still to be split. */
  public boolean splitsShortOptions() {

    return this.phase.splitsShortOptions();
  }

  private void setCurrent(CliArgument arg) {

    if (arg.isEndOptions()) {
      endOptionsByToken();
      if (this.preserveEndOptionsToken) {
        this.currentArg = arg;
      } else {
        this.currentArg = arg.getNext();
      }
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
      setCurrent(this.currentArg.getNext(this.phase.splitsShortOptions()));
    }
    return this.currentArg;
  }

  /**
   * @return a copy of this {@link CliArguments} to fork a CLI matching of auto-completion.
   */
  public CliArguments copy() {
    return new CliArguments(this.currentArg, this.phase, this.preserveEndOptionsToken);
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

    CliArguments ca = new CliArguments(CliArgument.ofCompletion(args));
    ca.preserveEndOptionsToken = true;
    return ca;
  }

  /**
   * Enable preserving the literal end-of-options token "--" so it is not consumed by the parser. This is used for completion mode where the user input should
   * be preserved verbatim.
   */
  public void preserveEndOptionsToken() {
    this.preserveEndOptionsToken = true;
  }

}
