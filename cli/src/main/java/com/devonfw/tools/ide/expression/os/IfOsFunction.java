package com.devonfw.tools.ide.expression.os;

import java.util.function.Predicate;

import com.devonfw.tools.ide.expression.AbstractExpressionFunction;
import com.devonfw.tools.ide.expression.ExpressionContext;
import com.devonfw.tools.ide.expression.ExpressionFunction;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.property.StringProperty;

/**
 * {@link ExpressionFunction} {@code @if-windows}, {@code @if-mac}, {@code @if-linux} and {@code @if-unix}.
 * <ol>
 * <li>the text to insert if the operating system matches. Otherwise the expression resolves to the empty string.</li>
 * </ol>
 */
public abstract class IfOsFunction extends AbstractExpressionFunction {

  private final String name;

  private final Predicate<SystemInfo> condition;

  private final StringProperty value;

  protected IfOsFunction(String name, Predicate<SystemInfo> condition) {

    super();
    this.name = name;
    this.condition = condition;
    this.value = add(new StringProperty("value", true));
  }

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  protected String execute(ExpressionContext context) {

    if (this.condition.test(context.getIdeContext().getSystemInfo())) {
      return this.value.getValue();
    }
    return "";
  }

}
