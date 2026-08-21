package com.devonfw.tools.ide.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an OS native package manager used for managing software packages.
 */
public enum NativePackageManager {
  /** Advanced Package Tool (APT) is the package manager of Debian based Linux distributions. */
  APT("install -y", "-y autoremove --purge", "=", "*"),

  /** Zypper is the package manager of SUSE based Linux distributions. */
  ZYPPER("--non-interactive install", "remove", "=", ""),

  /** Yellowdog Updater Modified (YUM) is the package manager of RPM package based Linux distributions like Fedora, Red Hat, or CentOS. */
  YUM("install -y", "remove -y", "-", "*"),

  /** DaNdiFied yum (DNF) is the package manager of RPM package based Linux distributions like Fedora. It is the successor of {@link #YUM}. */
  DNF("install -y", "remove -y", "-", "*");

  private static final String DPKG_STATUS_INSTALLED = "installed";
  private static final String SUDO = "sudo";

  private final String installCommand;
  private final String uninstallCommand;
  private final String versionSeparator;
  private final String versionWildCard;

  NativePackageManager(String installCommand, String uninstallCommand, String versionSeparator, String versionWildCard) {
    this.installCommand = installCommand;
    this.uninstallCommand = uninstallCommand;
    this.versionSeparator = versionSeparator;
    this.versionWildCard = versionWildCard;
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
   * Builds the package specification pinning the given package to the given version.
   *
   * @param pkg the name of the package.
   * @param version the version to pin the package to or {@code null} to use the latest available version.
   * @return the package specification for this package manager.
   */

  public String getPackageSpec(String pkg, String version) {
    if ((version == null) || version.isBlank()) {
      return pkg;
    }
    String spec = pkg + this.versionSeparator + version + this.versionWildCard;
    if (this.versionWildCard.isEmpty()) {
      return spec;
    }
    return "'" + spec + "'";
  }

  /**
   * @param pkg the name of the package to query.
   * @return the command to determine the installed version of the given package as executable followed by its arguments.
   */
  public List<String> getVersionQueryCommand(String pkg) {
    List<String> command = new ArrayList<>(switch (this) {
      case APT -> List.of("dpkg-query", "-W", "-f=${db:Status-Status}|${Version}");
      case ZYPPER, YUM, DNF -> List.of("rpm", "-q", "--queryformat", "%{VERSION}");
    });
    command.add(pkg);
    return command;
  }

  /**
   * @param output the output of the {@link #getVersionQueryCommand(String) version query command}.
   * @return the version of the installed package or {@code null} if the package is not installed.
   */
  public String parseVersionQueryOutput(String output) {
    if ((output == null) || output.isBlank()) {
      return null;
    }
    String version = output.trim();
    if (this == APT) {
      String[] parts = version.split("\\|", 2);
      if ((parts.length != 2) || !DPKG_STATUS_INSTALLED.equals(parts[0].trim())) {
        return null;
      }
      version = parts[1].trim();
    }
    return version.isEmpty() ? null : version;
  }

  /**
   * @param version the version of the package to install.
   * @param nativePackage the {@link NativePackage} to install.
   * @return the {@link PackageManagerCommand} to install the given {@link NativePackage} including its {@link NativePackage#getSetupCommands()} setup commands.
   */
  public PackageManagerCommand install(NativePackage nativePackage, String version) {
    verifyPackageManager(nativePackage);
    List<String> commands = new ArrayList<>(nativePackage.getSetupCommands());
    StringBuilder command = new StringBuilder(SUDO).append(' ').append(getBinaryName());
    for (String option : nativePackage.getExtraInstallOptions()) {
      command.append(' ').append(option);
    }
    command.append(' ').append(this.installCommand);
    for (String pkg : nativePackage.getPackages()) {
      command.append(' ').append(getPackageSpec(pkg, version));
    }
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
    StringBuilder command = new StringBuilder(SUDO).append(' ').append(getBinaryName()).append(' ').append(this.uninstallCommand);
    for (String pkg : nativePackage.getPackages()) {
      command.append(' ').append(pkg);
    }
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
}
