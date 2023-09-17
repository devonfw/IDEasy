package com.devonfw.tools.ide.configurator.resolve;

/**
 * Interface for a resolver of variables. It is capable of replacing one or multiple occurrences of variables with their
 * actual value.
 *
 * @see #resolve(String)
 *
 * @since 3.0.0
 */
public interface VariableResolver {

  /**
   * @deprecated Legacy that is only supported for compatibility. Please use {@link #VARIABLE_DEVON_IDE_HOME} instead.
   */
  @Deprecated
  String VARIABLE_CLIENT_ENV_HOME = "client.env.home";

  /** Variable for top-level directory of 'devonfw-ide' installation (DEVON_IDE_HOME). */
  String VARIABLE_DEVON_IDE_HOME = "DEVON_IDE_HOME";

  /** Variable for directory of the current workspace (WORKSPACE_PATH). */
  String VARIABLE_WORKSPACE_PATH = "WORKSPACE_PATH";

  /**
   * @param text the {@link String} to resolve (e.g. value of {@link java.util.Properties#get(Object) property} or from
   *        XML attribute or element).
   * @return the resolved {@link String}. If the given {@link String} contained variables (e.g. "${«variable.name»}"),
   *         these are resolved and replaced with their actual value.
   */
  String resolve(String text);

  /**
   * Inverse operation of {@link #resolve(String)}.
   *
   * @param text the {@link String} to generalize (substitute values back to variable expressions).
   * @return the generalized {@link String}. If the given {@link String} contained values of the variables (e.g. an
   *         absolute path), these are substituted back to variable expressions.
   */
  String inverseResolve(String text);

}
