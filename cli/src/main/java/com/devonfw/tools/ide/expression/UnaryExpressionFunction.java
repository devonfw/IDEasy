package com.devonfw.tools.ide.expression;

import java.util.List;

/**
 * Abstract base implementation of {@link ExpressionFunction} for a function that takes exactly one required argument. Avoids every such function having to
 * check the argument count and index into the {@link List} of arguments itself.
 */
public abstract class UnaryExpressionFunction implements ExpressionFunction {

  @Override
  public final int getMinArgs() {

    return 1;
  }

  @Override
  public final int getMaxArgs() {

    return 1;
  }

  @Override
  public final String apply(List<String> args, ExpressionContext context) {

    return apply(args.get(0), context);
  }

  /**
   * @param arg the single argument of this function.
   * @param context the {@link ExpressionContext}.
   * @return the result of this function that will replace the entire expression.
   */
  protected abstract String apply(String arg, ExpressionContext context);

}
