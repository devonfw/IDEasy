package com.devonfw.tools.ide.expression;

import java.util.List;

/**
 * Abstract base implementation of {@link ExpressionFunction} for a function that takes up to two arguments. Avoids every such function having to check the
 * argument count and index into the {@link List} of arguments itself: missing optional trailing arguments are simply passed as {@code null}.
 */
public abstract class BinaryExpressionFunction implements ExpressionFunction {

  private final int minArgs;

  /**
   * The constructor for a function where both arguments are required.
   */
  protected BinaryExpressionFunction() {

    this(2);
  }

  /**
   * The constructor.
   *
   * @param minArgs the {@link #getMinArgs() minimum number of arguments} (1 if the 2nd argument is optional, 2 if it is required).
   */
  protected BinaryExpressionFunction(int minArgs) {

    super();
    if ((minArgs < 1) || (minArgs > 2)) {
      throw new IllegalArgumentException("minArgs has to be 1 or 2 but was " + minArgs);
    }
    this.minArgs = minArgs;
  }

  @Override
  public final int getMinArgs() {

    return this.minArgs;
  }

  @Override
  public final int getMaxArgs() {

    return 2;
  }

  @Override
  public final String apply(List<String> args, ExpressionContext context) {

    String arg1 = args.get(0);
    String arg2 = (args.size() > 1) ? args.get(1) : null;
    return apply(arg1, arg2, context);
  }

  /**
   * @param arg1 the 1st argument of this function.
   * @param arg2 the 2nd argument of this function, or {@code null} if not given and {@link #getMinArgs() optional}.
   * @param context the {@link ExpressionContext}.
   * @return the result of this function that will replace the entire expression.
   */
  protected abstract String apply(String arg1, String arg2, ExpressionContext context);

}
