package com.devonfw.tools.ide.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents an argument for a command-line-interface (CLI) and a chain to all its {@link #getNext(boolean)
 * successors}.
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

  /** A {@link CliArgument} to represent the end of the CLI arguments. */
  public static final CliArgument END = new CliArgument();

  static final String NAME_START = "«start»";

  private final String arg;

  private String key;

  private String value;

  final CliArgument next;

  private final boolean completion;

  private CliArgument() {

    super();
    this.arg = "«end»";
    this.next = null;
    this.completion = false;
  }

  /**
   * The constructor.
   *
   * @param arg the {@link #get() argument}.
   * @param next the {@link #getNext() next}.
   */
  protected CliArgument(String arg, CliArgument next) {

    this(arg, next, false);
  }

  /**
   * The constructor.
   *
   * @param arg the {@link #get() argument}.
   * @param next the {@link #getNext() next}.
   * @param completion the {@link #isCompletion() completion flag}.
   */
  protected CliArgument(String arg, CliArgument next, boolean completion) {

    super();
    Objects.requireNonNull(arg);
    Objects.requireNonNull(next);
    this.arg = arg;
    this.next = next;
    this.completion = completion;
  }

  /**
   * @return {@code true} if this is the argument to complete (should be the last one), {@code false} otherwise.
   */
  public boolean isCompletion() {

    return this.completion;
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
   * @return {@code true} if this is a short option (e.g. "-b"), {@code false} otherwise.
   */
  public boolean isShortOption() {

    return (this.arg.length() >= 2) && (this.arg.charAt(0) == '-') && (this.arg.charAt(1) != '-');
  }

  /**
   * @return {@code true} if this is a combined short option (e.g. "-bd"), {@code false} otherwise.
   */
  public boolean isCombinedShortOption() {

    return (this.arg.length() > 2) && (this.arg.charAt(0) == '-') && (this.arg.charAt(1) != '-');
  }

  /**
   * @return {@code true} if {@link #END_OPTIONS}, {@code false} otherwise.
   */
  public boolean isEndOptions() {

    return this.arg.equals(END_OPTIONS);
  }

  /**
   * @return {@code true} if this is the {@link #END} of the arguments, {@code false} otherwise.
   */
  public boolean isEnd() {

    return (this.next == null);
  }

  /**
   * @return {@code true} if this is the start of the arguments, {@code false} otherwise.
   */
  public boolean isStart() {

    return (this.arg == NAME_START); // not using equals on purpose
  }

  /**
   * @param successors the number of {@link #getNext() successors} expected.
   * @return {@code true} if at least the given number of {@link #getNext() successors} are available, {@code false}
   *         otherwise.
   */
  public boolean hasMoreSuccessorsThan(int successors) {

    if (successors <= 0) {
      return true;
    }
    CliArgument current = this;
    while (current != END) {
      successors--;
      if (successors == 0) {
        return true;
      }
      current = current.next;
    }
    return false;
  }

  /**
   * @return the next {@link CliArgument} or {@code null} if this is the {@link #isEnd() end}.
   * @see #getNext(boolean)
   */
  public CliArgument getNext() {

    return this.next;
  }

  /**
   * @param splitShortOpts - if {@code true} then combined short options will be split (so instead of "-fbd" you will
   *        get "-f", "-b", "-d").
   * @return the next {@link CliArgument} or {@code null} if this is the {@link #isEnd() end}.
   */
  public CliArgument getNext(boolean splitShortOpts) {

    if (splitShortOpts && (this.next != null) && !this.next.completion) {
      String option = this.next.arg;
      int len = option.length();
      if ((len > 2) && (option.charAt(0) == '-') && (option.charAt(1) != '-')) {
        CliArgument current = this.next.next;
        for (int i = len - 1; i > 0; i--) {
          current = new CliArgument("-" + option.charAt(i), current);
        }
        return current;
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
    if (current.isStart()) {
      current = current.next;
    }
    String prefix = "\"";
    while (!current.isEnd()) {
      sb.append(prefix);
      sb.append(current.arg);
      sb.append("\"");
      current = current.next;
      prefix = " \"";
    }
    return sb.toString();
  }

  private CliArgument createStart() {

    assert (!isStart());
    return new CliArgument(NAME_START, this);
  }

  /**
   * @return a {@link String} array with all arguments starting from this one.
   */
  public String[] asArray() {

    List<String> args = new ArrayList<>();
    CliArgument current = this;
    while (!current.isEnd()) {
      args.add(current.arg);
      current = current.next;
    }
    return args.toArray(size -> new String[size]);
  }

  @Override
  public String toString() {

    return this.arg;
  }

  /**
   * @param args the command-line arguments (e.g. from {@code main} method).
   * @return the first {@link CliArgument} of the parsed arguments or {@code null} if for empty arguments.
   */
  public static CliArgument of(String... args) {

    return of(false, args);
  }

  /**
   * @param args the command-line arguments (e.g. from {@code main} method).
   * @return the first {@link CliArgument} of the parsed arguments or {@code null} if for empty arguments.
   */
  public static CliArgument ofCompletion(String... args) {

    return of(true, args);
  }

  private static CliArgument of(boolean completion, String... args) {

    CliArgument current = CliArgument.END;
    int last = args.length - 1;
    for (int argsIndex = last; argsIndex >= 0; argsIndex--) {
      String arg = args[argsIndex];
      boolean completionArg = false;
      if (argsIndex == last) {
        completionArg = completion;
      }
      current = new CliArgument(arg, current, completionArg);
    }
    return current.createStart();
  }

  /**
   * @param firstArgs the first arguments.
   * @param nextArgs the additional arguments to append after {@code args}.
   * @return a {@link String} array with the values from {@code firstArgs} followed by the values from {@code nextArgs}.
   */
  public static String[] append(String[] firstArgs, String... nextArgs) {

    return join(firstArgs, false, nextArgs);
  }

  /**
   * @param nextArgs the arguments to append after {@code firstArgs}.
   * @param firstArgs the first arguments.
   * @return a {@link String} array with the values from {@code firstArgs} followed by the values from {@code nextArgs}.
   */
  public static String[] prepend(String[] nextArgs, String... firstArgs) {

    return join(nextArgs, false, firstArgs);
  }

  private static String[] join(String[] args, boolean prefix, String... extraArgs) {

    String[] result = new String[args.length + extraArgs.length];
    int argsStart = 0;
    int extraArgsStart = args.length;
    if (prefix) {
      argsStart = extraArgs.length;
      extraArgsStart = 0;
    }
    System.arraycopy(args, 0, result, argsStart, args.length);
    System.arraycopy(extraArgs, 0, result, extraArgsStart, extraArgs.length);
    return result;
  }
}
