package com.devonfw.tools.ide.expression.os;

import com.devonfw.tools.ide.os.SystemInfo;

/**
 * {@link IfOsFunction} for Linux.
 */
public class IfLinuxFunction extends IfOsFunction {

  /**
   * The constructor.
   */
  public IfLinuxFunction() {
    super("if-linux", SystemInfo::isLinux);
  }
}
