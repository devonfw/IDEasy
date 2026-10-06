package com.devonfw.tools.ide.tool;

/**
 * The kind of operation performed on a {@link NativePackage} via its {@link NativePackageManager}.
 */
public enum NativePackageAction {
  /** Installation of a {@link NativePackage}. */
  INSTALL("installed"),
  /** Uninstallation of a {@link NativePackage}. */
  UNINSTALL("uninstalled");

  private final String action;

  NativePackageAction(String action) {
    this.action = action;
  }

  /**
   * @return the action used in log messages.
   */
  public String getAction() {
    return action;
  }
}
