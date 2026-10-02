package com.devonfw.tools.ide.expression;

import java.util.List;

/**
 * Abstract base implementation of {@link ExpressionFunction} for a function that takes up to four arguments. Avoids every such function having to check the
 * argument count and index into the {@link List} of arguments itself: missing optional trailing arguments are simply passed as {@code null}.
 */
public abstract class QuaternaryExpressionFunction implements ExpressionFunction {

  private final int minArgs;

  /**
   * The constructor.
   *
   * @param minArgs the {@link #getMinArgs() minimum number of arguments} (1 to 4, depending on how many trailing arguments are optional).
   */
  protected QuaternaryExpressionFunction(int minArgs) {

    super();
    if ((minArgs < 1) || (minArgs > 4)) {
      throw new IllegalArgumentException("minArgs has to be between 1 and 4 but was " + minArgs);
    }
    this.minArgs = minArgs;
  }

  @Override
  public final int getMinArgs() {

    return this.minArgs;
  }

  @Override
  public final int getMaxArgs() {

    return 4;
  }

  @Override
  public final String apply(List<String> args, ExpressionContext context) {

    int size = args.size();
    String arg1 = args.get(0);
    String arg2 = (size > 1) ? args.get(1) : null;
    String arg3 = (size > 2) ? args.get(2) : null;
    String arg4 = (size > 3) ? args.get(3) : null;
    return apply(arg1, arg2, arg3, arg4, context);
  }

  /**
   * @param arg1 the 1st argument of this function.
   * @param arg2 the 2nd argument of this function, or {@code null} if not given and {@link #getMinArgs() optional}.
   * @param arg3 the 3rd argument of this function, or {@code null} if not given and {@link #getMinArgs() optional}.
   * @param arg4 the 4th argument of this function, or {@code null} if not given and {@link #getMinArgs() optional}.
   * @param context the {@link ExpressionContext}.
   * @return the result of this function that will replace the entire expression.
   */
  protected abstract String apply(String arg1, String arg2, String arg3, String arg4, ExpressionContext context);

}
