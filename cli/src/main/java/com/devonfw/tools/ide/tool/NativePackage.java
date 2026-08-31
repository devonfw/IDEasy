package com.devonfw.tools.ide.tool;

import java.util.List;
import java.util.Objects;

/**
 * Represents the OS native packages a {@link GlobalToolCommandlet} consists of for a specific {@link NativePackageManager}.
 */
public class NativePackage {

  private final NativePackageManager packageManager;
  private final List<String> packages;
  private final List<String> extraInstallOptions;
  private final List<String> setupCommands;
  private final List<String> cleanupCommands;

  /**
   * Creates a new {@link NativePackage} with optional fields defaulting to empty lists.
   *
   * @param pm the specific {@link NativePackageManager}
   * @param packages the packages that need to be handled.
   * @param extraInstallOptions extra install options (optional)
   * @param setupCommands commands to run before install (optional)
   * @param cleanupCommands commands to run after uninstall (optional)
   */
  public NativePackage(NativePackageManager pm, List<String> packages,
      List<String> extraInstallOptions, List<String> setupCommands, List<String> cleanupCommands) {
    this.packageManager = Objects.requireNonNull(pm, "package manager must not be null");
    this.packages = List.copyOf(Objects.requireNonNull(packages, "packages must not be null"));
    this.extraInstallOptions = extraInstallOptions != null ? List.copyOf(extraInstallOptions) : List.of();
    this.setupCommands = setupCommands != null ? List.copyOf(setupCommands) : List.of();
    this.cleanupCommands = cleanupCommands != null ? List.copyOf(cleanupCommands) : List.of();
  }

  /**
   * Convenience constructor for the common case: package manager + packages.
   *
   * @param pm the specific {@link NativePackageManager}
   * @param packages the packages that need to be handled
   */
  public NativePackage(NativePackageManager pm, List<String> packages) {
    this(pm, packages, null, null, null);
  }

  /**
   * Factory method for simple packages.
   *
   * @param pm the specific {@link NativePackageManager}
   * @param packages the packages that need to be handled.
   * @return a new {@link NativePackage} with given params.
   */
  public static NativePackage of(NativePackageManager pm, String... packages) {
    return new NativePackage(pm, List.of(packages));
  }

  /**
   * @return {@link packages} that are handled.
   */
  public List<String> getPackages() {
    return packages;
  }

  /**
   * @return set {@link NativePackageManager}.
   */
  public NativePackageManager getPackageManager() {
    return packageManager;
  }

  /**
   * @return set {@link extraInstallOptions}.
   */
  public List<String> getExtraInstallOptions() {
    return extraInstallOptions;
  }

  /**
   * @return set {@link setupCommands}.
   */
  public List<String> getSetupCommands() {
    return setupCommands;
  }

  /**
   * @return set {@link cleanupCommands}.
   */
  public List<String> getCleanupCommands() {
    return cleanupCommands;
  }

  /**
   * @param version the version to pin the {@link #getPackages()} to or {@code null} to install the latest available version.
   * @return {@link PackageManagerCommand} for installation.
   */
  public PackageManagerCommand install(String version) {
    return this.packageManager.install(this, version);
  }

  /**
   * @return {@link PackageManagerCommand} for uninstallation.
   */
  public PackageManagerCommand uninstall() {
    return this.packageManager.uninstall(this);
  }

  /**
   * @return the command to determine the installed version of the leading {@link #getPackages() package} as executable followed by its arguments.
   */
  public List<String> getVersionQueryCommand() {
    return this.packageManager.getVersionQueryCommand(this.packages.getFirst());
  }
}
