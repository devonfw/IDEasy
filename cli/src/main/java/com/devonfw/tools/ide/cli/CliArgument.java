package com.devonfw.tools.ide.cli;

/**
 * A single argument of a {@code main} method from a command-line-interface (CLI).
 *
 * @since 1.0.0
 * @see #getNext(boolean)
 */
public class CliArgument {

  /**
   * The {@link #get() argument} to indicate the end of the options. If this string is given as argument, any further
   * arguments are treated as values. This allows to provide values (e.g. a filename) starting with a hyphen ('-').
   */
  public static final String END_OPTIONS = "--";

  /** A dummy {@link CliArgument} to represent the end of the CLI arguments. */
  public static final CliArgument END = new CliArgument("«end»");

  private final String arg;

  private String key;

  private String value;

  private CliArgument next;

  /**
   * The constructor.
   *
   * @param arg the {@link #get() argument}.
   */
  protected CliArgument(String arg) {

    super();
    this.arg = arg;
    this.next = END;
  }

  /**
   * @return the argument text (e.g. "-h" for a short option, "--help" for a long option, or "foo" for a value).
   */
  public String get() {

    return this.arg;
  }

  /**
   * @return {@code true} if this is an option (e.g. "-h" or "--help"), {@code false} otherwise.
   */
  public boolean isOption() {

    return this.arg.startsWith("-");
  }

  /**
   * @return {@code true} if this is a long option (e.g. "--help"), {@code false} otherwise.
   */
  public boolean isLongOption() {

    return this.arg.startsWith("--");
  }

  /**
   * @return {@code true} if {@link #END_OPTIONS}, {@code false} otherwise.
   */
  public boolean isEndOptions() {

    return this.arg.equals(END_OPTIONS);
  }

  /**
   * @return {@code true} if this is the {@link #END}, {@code false} otherwise.
   */
  public boolean isEnd() {

    return (this == END);
  }

  /**
   * @param splitShortOpts - if {@code true} then combined short options will be split (so instead of "-fbd" you will
   *        get "-f", "-b", "-d").
   * @return the next {@link CliArgument} or {@code null} if this is the last argument.
   */
  public CliArgument getNext(boolean splitShortOpts) {

    if (splitShortOpts && (this.next != null)) {
      String option = this.next.arg;
      int len = option.length();
      if ((len > 2) && (option.charAt(0) == '-') && (option.charAt(1) != '-')) {
        CliArgument first = null;
        CliArgument current = null;
        for (int i = 1; i < len; i++) {
          CliArgument shortOpt = new CliArgument("-" + option.charAt(i));
          shortOpt.next = this.next.next;
          if (current == null) {
            first = shortOpt;
          } else {
            current.next = shortOpt;
          }
          current = shortOpt;
        }
        return first;
      }
    }
    return this.next;
  }

  /**
   * @return the {@code «key»} part if the {@link #get() argument} has the has the form {@code «key»=«value»}. Otherwise
   *         the {@link #get() argument} itself.
   */
  public String getKey() {

    initKeyValue();
    return this.key;
  }

  /**
   * @return the {@code «value»} part if the {@link #get() argument} has the has the form {@code «key»=«value»}.
   *         Otherwise {@code null}.
   */
  public String getValue() {

    initKeyValue();
    return this.value;
  }

  private void initKeyValue() {

    if (this.key != null) {
      return;
    }
    int equalsIndex = this.arg.indexOf('=');
    if (equalsIndex < 0) {
      this.key = this.arg;
    } else {
      this.key = this.arg.substring(0, equalsIndex);
      this.value = this.arg.substring(equalsIndex + 1);
    }
  }

  /**
   * @return a {@link String} representing all arguments from this {@link CliArgument} recursively along is
   *         {@link #getNext(boolean) next} arguments to the {@link #isEnd() end}.
   */
  public String getArgs() {

    if (isEnd()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    CliArgument current = this;
    String prefix = "\"";
    while (!current.isEnd()) {
      sb.append(prefix);
      sb.append(this.arg);
      sb.append("\"");
      current = current.next;
      prefix = " \"";
    }
    return sb.toString();
  }

  @Override
  public String toString() {

    return this.arg;
  }

  /**
   * @param args the command-line arguments from {@code main} method.
   * @return the first {@link CliArgument} of the parsed arguments or {@code null} if for empty arguments.
   */
  public static CliArgument of(String... args) {

    return of(true, args);
  }

  /**
   * @param splitShortOpt - to {@link #getNext(boolean) split combined short options} for the first argument.
   * @param args the command-line arguments from {@code main} method.
   * @return the first {@link CliArgument} of the parsed arguments or {@code null} if for empty arguments.
   */
  public static CliArgument of(boolean splitShortOpt, String... args) {

    CliArgument first = CliArgument.END;
    CliArgument current = null;
    for (int argsIndex = 0; argsIndex < args.length; argsIndex++) {
      String arg = args[argsIndex];
      CliArgument argument = new CliArgument(arg);
      if (current == null) {
        first = argument;
        current = argument;
        if (splitShortOpt) {
          CliArgument start = new CliArgument("");
          start.next = argument;
          first = start.getNext(true);
          current = first;
          while (!current.next.isEnd()) {
            current = current.next;
          }
        }
      } else {
        if (current.isEnd()) {
          // should never happen, but if a bug leads us here it is severe
          throw new IllegalStateException("Internal error!");
        }
        current.next = argument;
        current = argument;
      }
    }
    return first;
  }

}
