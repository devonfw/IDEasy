package com.devonfw.tools.ide.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for expressions of the syntax {@code @«function-name»([«arg»[,«arg»]*])}.
 * <p>
 * A regular expression is only used to <em>locate</em> the start of a function call. The argument list is then scanned
 * manually, because a regular expression cannot express a balanced list of an arbitrary number of arguments that may
 * contain quoted commas, quoted parenthesis or nested function calls.
 * <p>
 * Text that does not form a call of a {@link ExpressionFunctionManager#getFunction(String) registered function} is
 * passed through entirely untouched. This is essential since foreign configuration formats may use an {@code @} for
 * their own purposes (e.g. CSS {@code @media(...)}) and IDEasy must never try to resolve placeholders that are not
 * ours.
 */
public class ExpressionParser {

  private static final Logger LOG = LoggerFactory.getLogger(ExpressionParser.class);

  /** Locates the start of a potential function call. The group is the function name. */
  //                                                       .1
  private static final Pattern FUNCTION_START = Pattern.compile("@([a-z][a-z0-9-]*)\\(");

  private static final int EXTRA_CAPACITY = 8;

  private final ExpressionFunctionManager functionManager;

  /**
   * The constructor.
   *
   * @param functionManager the {@link ExpressionFunctionManager}.
   */
  public ExpressionParser(ExpressionFunctionManager functionManager) {

    super();
    this.functionManager = functionManager;
  }

  /**
   * @param value the value potentially containing expressions.
   * @param context the {@link ExpressionContext}.
   * @return the given value with all expressions of registered functions replaced by their result.
   */
  public String resolve(String value, ExpressionContext context) {

    if (value == null) {
      return null;
    }
    Matcher matcher = FUNCTION_START.matcher(value);
    if (!matcher.find()) {
      return value;
    }
    StringBuilder sb = new StringBuilder(value.length() + EXTRA_CAPACITY);
    int pos = 0;
    while (matcher.find(pos)) {
      int start = matcher.start();
      int open = matcher.end() - 1;
      String functionName = matcher.group(1);
      int close = findClosingParenthesis(value, open);
      ExpressionFunction function = (close < 0) ? null : this.functionManager.getFunction(functionName);
      if (function == null) {
        LOG.trace("Ignoring '@{}(' in '{}' as it is no registered expression function.", functionName, value);
        sb.append(value, pos, matcher.end());
        pos = matcher.end();
        continue;
      }
      sb.append(value, pos, start);
      List<String> args = parseArguments(value, open + 1, close, context);
      sb.append(apply(function, args, value, context));
      pos = close + 1;
    }
    sb.append(value, pos, value.length());
    return sb.toString();
  }

  private String apply(ExpressionFunction function, List<String> args, String value, ExpressionContext context) {

    int size = args.size();
    int min = function.getMinArgs();
    int max = function.getMaxArgs();
    if ((size < min) || ((max >= 0) && (size > max))) {
      throw new IllegalArgumentException(
          "Function @" + function.getName() + " requires " + min + (max < 0 ? " or more" : " to " + max)
              + " argument(s) but received " + size + " in '" + value + "'.");
    }
    String result = function.apply(args, context);
    return (result == null) ? "" : result;
  }

  /**
   * @param value the value to scan.
   * @param open the index of the opening parenthesis.
   * @return the index of the matching closing parenthesis or {@code -1} if unbalanced.
   */
  private static int findClosingParenthesis(String value, int open) {

    int depth = 0;
    char quote = 0;
    for (int i = open; i < value.length(); i++) {
      char c = value.charAt(i);
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if ((c == '\'') || (c == '"')) {
        quote = c;
      } else if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Splits the argument list at top-level commas, then trims, unquotes and resolves each argument.
   *
   * @param value the entire value.
   * @param begin the index after the opening parenthesis.
   * @param end the index of the closing parenthesis (exclusive).
   * @param context the {@link ExpressionContext}.
   * @return the {@link List} of arguments.
   */
  private static List<String> parseArguments(String value, int begin, int end, ExpressionContext context) {

    List<String> args = new ArrayList<>();
    if (value.substring(begin, end).isBlank()) {
      return args;
    }
    int depth = 0;
    char quote = 0;
    int argStart = begin;
    for (int i = begin; i < end; i++) {
      char c = value.charAt(i);
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if ((c == '\'') || (c == '"')) {
        quote = c;
      } else if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      } else if ((c == ',') && (depth == 0)) {
        args.add(parseArgument(value.substring(argStart, i), context));
        argStart = i + 1;
      }
    }
    args.add(parseArgument(value.substring(argStart, end), context));
    return args;
  }

  private static String parseArgument(String arg, ExpressionContext context) {

    String result = arg.trim();
    int length = result.length();
    if (length >= 2) {
      char first = result.charAt(0);
      if (((first == '\'') || (first == '"')) && (result.charAt(length - 1) == first)) {
        result = result.substring(1, length - 1);
      }
    }
    return context.resolve(result);
  }

}
