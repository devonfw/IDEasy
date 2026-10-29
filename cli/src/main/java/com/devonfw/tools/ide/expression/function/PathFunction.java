package com.devonfw.tools.ide.expression.function;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.expression.BinaryExpressionFunction;
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
public class PathFunction extends BinaryExpressionFunction {

  /** The literal value for the second argument to normalise to unix syntax (default). */
  public static final String MODE_UNIX = "unix";

  /** The literal value for the second argument to normalise to the syntax native to the current operating system. */
  public static final String MODE_NATIVE = "native";

  /**
   * The constructor.
   */
  public PathFunction() {

    super(1);
  }

  @Override
  public String getName() {

    return "path";
  }

  @Override
  protected String apply(String path, String mode, ExpressionContext context) {

    if (mode == null) {
      mode = MODE_UNIX;
    }
    if (MODE_UNIX.equals(mode)) {
      return path.replace('\\', '/');
    } else if (MODE_NATIVE.equals(mode)) {
      if (context.getIdeContext().getSystemInfo().isWindows()) {
        return WindowsPathSyntax.WINDOWS.normalize(path).replace('/', '\\');
      }
      return path.replace('\\', '/');
    }
    throw new CliException(
        "Invalid template expression: invalid mode '" + mode + "' for function @path - expected '" + MODE_UNIX + "' or '" + MODE_NATIVE + "'.");
  }

}
