package com.devonfw.tools.ide.expression;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;

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
   * Persists the given variable to the {@code ide.properties} of the given {@link EnvironmentVariablesType configuration location} so the user is not asked
   * again.
   *
   * @param name the name of the variable.
   * @param value the value to persist.
   * @param type the {@link EnvironmentVariablesType} determining the {@code ide.properties} to write to.
   */
  void setVariable(String name, String value, EnvironmentVariablesType type);

}
