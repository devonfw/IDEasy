package com.devonfw.tools.ide.tool;

public enum PackageManager {
  APT, ZYPPER, YUM, DNF;

  public static PackageManager extractPackageManager(String command) {
    if (command.contains("apt")) return APT;
    if (command.contains("yum")) return YUM;
    if (command.contains("zypper")) return ZYPPER;
    if (command.contains("dnf")) return DNF;

    throw new IllegalArgumentException("Unknown package manager in command: " + command);
  }

  public String getBinaryName() {
    return name().toLowerCase();
  }
}
