package com.devonfw.tools.ide.expression;

import com.devonfw.tools.ide.context.IdeContext;

/**
 * Interface for the context available to an {@link ExpressionFunction} while an expression is evaluated.
 */
public interface ExpressionContext {

  /**
   * @return the {@link IdeContext}.
   */
  IdeContext getIdeContext();

  /**
   * Resolves variables in the given value. Used to resolve arguments of a function that may themselves contain
   * variables or nested expressions (e.g. {@code @path('$[IDE_HOME]/software/node')}).
   *
   * @param value the value to resolve.
   * @return the given value with variables and nested expressions resolved.
   */
  String resolve(String value);

  /**
   * @param name the name of the variable.
   * @return the value of the variable or {@code null} if not defined in any level of the hierarchy.
   */
  String getVariable(String name);

  /**
   * Persists the given variable to the user local {@code conf/ide.properties} so the user is not asked again.
   * <p>
   * Only has an effect if {@link #isPersistent()} returns {@code true}.
   *
   * @param name the name of the variable.
   * @param value the value to persist.
   */
  void setVariable(String name, String value);

  /**
   * @return {@code true} if values acquired from the user should be {@link #setVariable(String, String) persisted}.
   *     This is the case for workspace templates that are re-applied on every {@code ide update}. For settings
   *     templates that are only instantiated once, this is {@code false}.
   */
  boolean isPersistent();

}
