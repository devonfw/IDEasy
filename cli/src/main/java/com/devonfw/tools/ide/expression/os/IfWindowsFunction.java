package com.devonfw.tools.ide.expression.os;

import com.devonfw.tools.ide.os.SystemInfo;

/**
 * {@link IfOsFunction} for Windows.
 */
public class IfWindowsFunction extends IfOsFunction {

  /**
   * The constructor.
   */
  public IfWindowsFunction() {
    super("if-windows", SystemInfo::isWindows);
  }
}
