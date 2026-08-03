package com.devonfw.tools.ide.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an OS native package manager used for managing software packages.
 */
public enum NativePackageManager {
  /** Advanced Package Tool (APT) is the package manager of Debian based Linux distributions. */
  APT("install -y", "autoremove -y", "="),

  /** Zypper is the package manager of SUSE based Linux distributions. */
  ZYPPER("--non-interactive install", "--non-interactive remove --clean-deps", "="),

  /** Yellowdog Updater Modified (YUM) is the package manager of RPM package based Linux distributions like Fedora, Red Hat, or CentOS. */
  YUM("install -y", "remove -y", "-"),

  /** DaNdiFied yum (DNF) is the package manager of RPM package based Linux distributions like Fedora. It is the successor of {@link #YUM}. */
  DNF("install -y", "remove -y", "-");

  private final String installCommand;
  private final String uninstallCommand;
  private final String versionSeparator;

  NativePackageManager(String installCommand, String uninstallCommand, String versionSeparator) {
    this.installCommand = installCommand;
    this.uninstallCommand = uninstallCommand;
    this.versionSeparator = versionSeparator;
  }

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

  /**
   * @return the character separating a package name from its version.
   */
  public String getVersionSeparator() {
    return this.versionSeparator;
  }

  /**
   * @param nativePackage the {@link NativePackage} to install.
   * @return the {@link PackageManagerCommand} to install the given {@link NativePackage} including its {@link NativePackage#getSetupCommands()} setup commands.
   */
  public PackageManagerCommand install(NativePackage nativePackage) {
    verifyPackageManager(nativePackage);
    List<String> commands = new ArrayList<>(nativePackage.getSetupCommands());
    StringBuilder command = new StringBuilder("sudo ").append(getBinaryName());
    for (String option : nativePackage.getExtraInstallOptions()) {
      command.append(' ').append(option);
    }
    command.append(' ').append(this.installCommand);
    appendPackages(command, nativePackage.getPackagesWithVersion());
    commands.add(command.toString());
    return new PackageManagerCommand(this, commands);
  }

  /**
   * @param nativePackage the {@link NativePackage} to uninstall
   * @return the {@link PackageManagerCommand} to uninstall the given {@link NativePackage} including its
   *     {@link NativePackage#getCleanupCommands() clean up commands}.
   */
  public PackageManagerCommand uninstall(NativePackage nativePackage) {
    verifyPackageManager(nativePackage);
    StringBuilder command = new StringBuilder("sudo ").append(getBinaryName()).append(' ').append(this.uninstallCommand);
    appendPackages(command, nativePackage.getPackages());
    List<String> commands = new ArrayList<>();
    commands.add(command.toString());
    commands.addAll(nativePackage.getCleanupCommands());
    return new PackageManagerCommand(this, commands);
  }

  private void verifyPackageManager(NativePackage nativePackage) {
    if (nativePackage.getPackageManager() != this) {
      throw new IllegalArgumentException(
          "Package" + nativePackage.getPackages() + " is declared for package manager " + nativePackage.getPackageManager() + " and cannot be handled by "
              + this);
    }
  }

  private static void appendPackages(StringBuilder command, List<String> packages) {
    for (String pkg : packages) {
      command.append(' ').append(pkg);
    }
  }
}
