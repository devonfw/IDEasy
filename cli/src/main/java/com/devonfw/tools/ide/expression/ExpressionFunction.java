package com.devonfw.tools.ide.expression;

import java.util.List;

/**
 * Interface for a function that can be used in an expression of a template variable definition.
 * <p>
 * The syntax of an expression is {@code @«function-name»([«arg»[,«arg»]*])}. Implementations are registered in the
 * {@link ExpressionFunctionManager}.
 *
 * @see ExpressionFunctionManager
 */
public interface ExpressionFunction {

  /**
   * @return the name of this function as used in the expression syntax (e.g. "path" for {@code @path(...)}). Has to match
   *     {@code [a-z][a-z0-9-]*}.
   */
  String getName();

  /**
   * @return the minimum number of arguments required by this function.
   */
  int getMinArgs();

  /**
   * @return the maximum number of arguments supported by this function or {@code -1} for an unlimited number.
   */
  int getMaxArgs();

  /**
   * @param args the {@link List} of arguments. Already trimmed, unquoted and with variables resolved. Guaranteed to
   *     satisfy {@link #getMinArgs()} and {@link #getMaxArgs()}.
   * @param context the {@link ExpressionContext}.
   * @return the result of this function that will replace the entire expression.
   */
  String apply(List<String> args, ExpressionContext context);

}
