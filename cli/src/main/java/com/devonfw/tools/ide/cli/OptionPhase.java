package com.devonfw.tools.ide.cli;

/**
 * The phase of CLI argument parsing, i.e. how the remaining {@link CliArgument arguments} are to be interpreted.
 */
public enum OptionPhase {

  /** Options are still parsed: they are matched against the declared properties and combined short options are split. */
  OPTIONS,

  /** A multi-valued value property has taken over the trailing region; declared options are still matched, but no short-option splitting happens. */
  PASSTHROUGH,

  /** A literal {@link CliArgument#isEndOptions() "--"} token has been seen; all further arguments are passed through verbatim, no option matching at all. */
  AFTER_TOKEN;

  /** @return {@code true} if the options have ended (either by a multi-valued property or a literal {@code "--"} token). */
  public boolean isOptionsEnded() {

    return this != OPTIONS;
  }

  /** @return {@code true} if combined short options (e.g. {@code -bdf}) are still to be split. */
  public boolean splitsShortOptions() {

    return this == OPTIONS;
  }

  /** @return {@code true} if the options were ended by a literal {@code "--"} token. */
  public boolean isEndedByToken() {

    return this == AFTER_TOKEN;
  }

  /** @return {@code true} if a {@link CliArgument#isOption() option look-alike} may still be matched against declared options. */
  public boolean matchesDeclaredOptions() {

    return this != AFTER_TOKEN;
  }
}
