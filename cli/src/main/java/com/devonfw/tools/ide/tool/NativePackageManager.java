package com.devonfw.tools.ide.tool;

/**
 * Represents an OS native package manager used for managing software packages.
 */
public enum NativePackageManager {
  /** Advanced Package Tool (APT) is the package manager of Debian based Linux distributions. */
  APT,

  /** Zypper is the package manager of SUSE based Linux distributions. */
  ZYPPER,

  /** Yellowdog Updater Modified (YUM) is the package manager of RPM package based Linux distributions like Fedora, Red Hat, or CentOS. */
  YUM,

  /** DaNdiFied yum (DNF) is the package manager of RPM package based Linux distributions like Fedora. It is the successor of {@link #YUM}. */
  DNF;

  /**
   * Extracts the package manager from the provided command string.
   *
   * @param command The command string to extract the package manager from.
   * @return The corresponding {@code PackageManager} based on the provided command string.
   * @throws IllegalArgumentException If the command string does not contain a recognized package manager.
   */
  public static NativePackageManager extractPackageManager(String command) {

    if (command.contains("apt")) {
      return APT;
    }
    if (command.contains("yum")) {
      return YUM;
    }
    if (command.contains("zypper")) {
      return ZYPPER;
    }
    if (command.contains("dnf")) {
      return DNF;
    }

    throw new IllegalArgumentException("Unknown package manager in command: " + command);
  }

  public String getBinaryName() {

    return name().toLowerCase();
  }
}
