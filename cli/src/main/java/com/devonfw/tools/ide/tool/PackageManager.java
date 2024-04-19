package com.devonfw.tools.ide.tool;

/**
 * Represents a package manager used for managing software packages.
 */
public enum PackageManager {
  APT, ZYPPER, YUM, DNF;

  /**
   * Extracts the package manager from the provided command string.
   *
   * @param command The command string to extract the package manager from.
   * @return The corresponding {@code PackageManager} based on the provided command string.
   * @throws IllegalArgumentException If the command string does not contain a recognized package manager.
   */
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
