package com.devonfw.tools.ide.log;

/**
 * Interface to {@link #formatArgument(Object) format arguments to log}.
 */
@FunctionalInterface
public interface IdeLogArgFormatter {

  /** The default instance to use as fallback. */
  IdeLogArgFormatter DEFAULT = arg -> arg == null ? null : arg.toString();

  /**
   * @param argument the argument to format.
   * @return the formatted argument as {@link String}.
   */
  String formatArgument(Object argument);

}
