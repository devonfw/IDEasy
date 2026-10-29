package com.devonfw.tools.ide.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.CharReader;

/**
 * Parser for expressions of the syntax {@code @«function-name»([«arg»[,«arg»]*])}.
 * <p>
 * A regular expression is only used to <em>locate</em> the start of a function call. The argument list is then scanned manually, because a regular expression
 * cannot express a balanced list of an arbitrary number of arguments that may contain quoted commas, quoted parenthesis or nested function calls.
 * <p>
 * Text that does not form a call of a {@link ExpressionFunctionManager#getFunction(String) registered function} is passed through entirely untouched. This is
 * essential since foreign configuration formats may use an {@code @} for their own purposes (e.g. CSS {@code @media(...)}) and IDEasy must never try to resolve
 * placeholders that are not ours.
 */
public class ExpressionParser {

  private static final Logger LOG = LoggerFactory.getLogger(ExpressionParser.class);

  /** Locates the start of a potential function call. The group is the function name. */
  //..............................................................1
  private static final Pattern FUNCTION_START = Pattern.compile("@([a-z][a-z0-9-]*)\\(");

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
    CharReader charReader = new CharReader(value);
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    do {
      int start = matcher.start();
      int end = matcher.end();
      charReader.setIndex(end);
      String functionName = matcher.group(1);
      ExpressionFunction function = this.functionManager.getFunction(functionName);
      if (function == null) {
        LOG.trace("Ignoring '@{}(' in '{}' as it is no registered expression function.", functionName, value);
        sb.append(value, pos, end);
        pos = end;
        continue;
      }
      List<String> args = parseArguments(charReader, context);
      int index = charReader.getIndex();
      if (args == null) {
        // syntax error, be tolerant and append unresolved
        sb.append(value, pos, index);
        pos = index;
      } else {
        sb.append(value, pos, start);
        sb.append(apply(function, args, value, context));
        pos = index;
      }
    } while (matcher.find(pos));
    sb.append(value, pos, value.length());
    return sb.toString();
  }

  private String apply(ExpressionFunction function, List<String> args, String value, ExpressionContext context) {

    function.reset();
    String result = function.apply(args, context);
    return (result == null) ? "" : result;
  }

  /**
   * Splits the argument list at top-level commas, then trims, unquotes and resolves each argument.
   *
   * @param context the {@link ExpressionContext}.
   * @return the {@link List} of arguments.
   */
  private static List<String> parseArguments(CharReader charReader, ExpressionContext context) {

    List<String> args = new ArrayList<>();
    while (charReader.hasNext()) {
      charReader.skipSpaces();
      boolean added = parseArgument(charReader, args, context);
      if (added) {
        charReader.skipSpaces();
        if (charReader.expect(',')) {
          continue;
        }
      }
      if (charReader.expect(')')) {
        break;
      }
      LOG.error("Illegal syntax found in expression arguments: {}", charReader.getRest());
      return null;
    }
    return args;
  }

  private static boolean parseArgument(CharReader charReader, List<String> args, ExpressionContext context) {

    if (charReader.expect("null")) {
      args.add(null);
      return true;
    }
    String param = charReader.readQuotedString();
    if (param != null) {
      param = context.resolve(param);
      args.add(param);
      return true;
    }
    return false;
  }

}
