package com.devonfw.tools.ide.expression.os;

import com.devonfw.tools.ide.os.SystemInfo;

/**
 * {@link IfOsFunction} for Mac.
 */
public class IfMacFunction extends IfOsFunction {

  /**
   * The constructor.
   */
  public IfMacFunction() {
    super("if-mac", SystemInfo::isMac);
  }
}
