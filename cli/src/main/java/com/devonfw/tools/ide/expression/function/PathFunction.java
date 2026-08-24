package com.devonfw.tools.ide.expression.function;

import java.util.List;

import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;
import com.devonfw.tools.ide.os.WindowsPathSyntax;

/**
 * {@link ExpressionFunction} {@code @path} that normalises a path.
 * <ol>
 * <li>the path to normalise. By default backslashes are replaced with slashes.</li>
 * <li>optional: the literal value {@code unix} (default) or {@code native}.</li>
 * </ol>
 * Example: {@code @path('$[IDE_HOME]/software/node/node.exe')}
 */
public class PathFunction implements ExpressionFunction {

  /** The literal value for the second argument to normalise to unix syntax (default). */
  public static final String MODE_UNIX = "unix";

  /** The literal value for the second argument to normalise to the syntax native to the current operating system. */
  public static final String MODE_NATIVE = "native";

  @Override
  public String getName() {

    return "path";
  }

  @Override
  public int getMinArgs() {

    return 1;
  }

  @Override
  public int getMaxArgs() {

    return 2;
  }

  @Override
  public String apply(List<String> args, ExpressionContext context) {

    String path = args.get(0);
    String mode = (args.size() > 1) ? args.get(1) : MODE_UNIX;
    if (MODE_UNIX.equals(mode)) {
      return path.replace('\\', '/');
    } else if (MODE_NATIVE.equals(mode)) {
      if (context.getIdeContext().getSystemInfo().isWindows()) {
        return WindowsPathSyntax.WINDOWS.normalize(path).replace('/', '\\');
      }
      return path.replace('\\', '/');
    }
    throw new IllegalArgumentException(
        "Invalid mode '" + mode + "' for function @path - expected '" + MODE_UNIX + "' or '" + MODE_NATIVE + "'.");
  }

}
