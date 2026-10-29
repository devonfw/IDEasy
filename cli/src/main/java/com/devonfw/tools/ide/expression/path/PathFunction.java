package com.devonfw.tools.ide.expression.path;

import com.devonfw.tools.ide.expression.AbstractExpressionFunction;
import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;
import com.devonfw.tools.ide.property.EnumProperty;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link ExpressionFunction} {@code @path} that normalises a path.
 * <ol>
 * <li>the path to normalise. By default backslashes are replaced with slashes.</li>
 * <li>optional: the literal value {@code unix} (default) or {@code native}.</li>
 * </ol>
 * Example: {@code @path('$[IDE_HOME]/software/node/node.exe')}
 */
public class PathFunction extends AbstractExpressionFunction {

  /** The literal value for the second argument to normalise to unix syntax (default). */
  public static final String MODE_UNIX = "unix";

  /** The literal value for the second argument to normalise to the syntax native to the current operating system. */
  public static final String MODE_NATIVE = "native";

  private final StringProperty path;

  private final EnumProperty<PathMode> mode;

  /**
   * The constructor.
   */
  public PathFunction() {

    super();
    this.path = add(new StringProperty("path", true));
    this.mode = add(new EnumProperty<>("mode", false, PathMode.class));
  }

  @Override
  public String getName() {

    return "path";
  }

  @Override
  protected String execute(int paramCount, ExpressionContext context) {

    PathMode pathMode = this.mode.getValueOrDefault(PathMode.UNIX);
    return pathMode.format(this.path.getValue(), context.getIdeContext().getSystemInfo());
  }

}
