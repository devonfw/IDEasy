package com.devonfw.tools.ide.expression;

import java.util.List;

import com.devonfw.tools.ide.function.ParameterizedFunction;

/**
 * Interface for a {@link ParameterizedFunction} that can be used in an expression of a template variable definition.
 * <p>
 * The syntax of an expression is {@code @«function-name»([«arg»[,«arg»]*])}. Implementations are registered in the {@link ExpressionFunctionManager}.
 *
 * @see ExpressionFunctionManager
 */
public interface ExpressionFunction extends ParameterizedFunction {

  /**
   * @param args the {@link List} of arguments. Already trimmed, unquoted and with variables resolved.
   * @param context the {@link ExpressionContext}.
   * @return the result of this function that will replace the entire expression.
   */
  String apply(List<Object> args, ExpressionContext context);

}
