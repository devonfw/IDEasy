package com.devonfw.tools.ide.expression.os;

/**
 * {@link IfOsFunction} for Unix (Linux or Mac).
 */
public class IfUnixFunction extends IfOsFunction {

  /**
   * The constructor.
   */
  public IfUnixFunction() {
    super("if-unix", systemInfo -> !systemInfo.isWindows());
  }
}
