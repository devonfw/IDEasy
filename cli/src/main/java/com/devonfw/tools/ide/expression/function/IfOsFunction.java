package com.devonfw.tools.ide.expression.function;

import java.util.List;
import java.util.function.Predicate;

import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;
import com.devonfw.tools.ide.expression.UnaryExpressionFunction;
import com.devonfw.tools.ide.os.SystemInfo;

/**
 * {@link ExpressionFunction} {@code @if-windows}, {@code @if-mac}, {@code @if-linux} and {@code @if-unix}.
 * <ol>
 * <li>the text to insert if the operating system matches. Otherwise the expression resolves to the empty string.</li>
 * </ol>
 */
public class IfOsFunction extends UnaryExpressionFunction {

  private final String name;

  private final Predicate<SystemInfo> condition;

  private IfOsFunction(String name, Predicate<SystemInfo> condition) {

    super();
    this.name = name;
    this.condition = condition;
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  protected String apply(String arg, ExpressionContext context) {

    if (this.condition.test(context.getIdeContext().getSystemInfo())) {
      return arg;
    }
    return "";
  }

  /**
   * @return all instances of this {@link ExpressionFunction}.
   */
  public static List<IfOsFunction> all() {

    return List.of( //
        new IfOsFunction("if-windows", SystemInfo::isWindows), //
        new IfOsFunction("if-mac", SystemInfo::isMac), //
        new IfOsFunction("if-linux", SystemInfo::isLinux), //
        new IfOsFunction("if-unix", systemInfo -> !systemInfo.isWindows()));
  }

}
