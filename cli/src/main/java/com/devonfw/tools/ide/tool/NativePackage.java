package com.devonfw.tools.ide.tool;

import java.util.List;
import java.util.Objects;

/**
 * Represents the OS native packages a {@link GlobalToolCommandlet} consists of for a specific {@link NativePackageManager}.
 */
public class NativePackage {

  private final NativePackageManager packageManager;
  private final List<String> packages;
  private final String version;
  private final List<String> extraInstallOptions;
  private final List<String> setupCommands;
  private final List<String> cleanupCommands;

  /**
   * Creates a new {@link NativePackage} with optional fields defaulting to empty lists.
   *
   * @param pm the specific {@link NativePackageManager}
   * @param packages the packages that need to be handled.
   * @param version the package version (optional).
   * @param extraInstallOptions extra install options (optional)
   * @param setupCommands commands to run before install (optional)
   * @param cleanupCommands commands to run after uninstall (optional)
   */
  public NativePackage(NativePackageManager pm, List<String> packages, String version,
      List<String> extraInstallOptions, List<String> setupCommands, List<String> cleanupCommands) {
    this.packageManager = Objects.requireNonNull(pm, "package manager must not be null");
    this.packages = List.copyOf(Objects.requireNonNull(packages, "packages must not be null"));
    this.version = version;
    this.extraInstallOptions = extraInstallOptions != null ? List.copyOf(extraInstallOptions) : List.of();
    this.setupCommands = setupCommands != null ? List.copyOf(setupCommands) : List.of();
    this.cleanupCommands = cleanupCommands != null ? List.copyOf(cleanupCommands) : List.of();
  }

  /**
   * Convenience constructor for the common case: package manager + packages.
   *
   * @param pm the specific {@link NativePackageManager}
   * @param packages the packages that need to be handled-
   */
  public NativePackage(NativePackageManager pm, List<String> packages) {
    this(pm, packages, null, null, null, null);
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
   * Factory method with version.
   *
   * @param pm the specific {@link NativePackageManager}
   * @param version the package version.
   * @param packages the packages that need to be handled.
   * @return a new {@link NativePackage} with given params.
   */
  public static NativePackage ofVersion(NativePackageManager pm, String version, String... packages) {
    return new NativePackage(pm, List.of(packages), version, null, null, null);
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
   * @return set {@link version}.
   */
  public String getVersion() {
    return version;
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
   * @return version of the package
   */
  public List<String> getPackagesWithVersion() {
    if ((this.version == null) || this.version.isBlank()) {
      return this.packages;
    }
    String versionSuffix = this.packageManager.getVersionSeparator() + this.version;
    return this.packages.stream().map(pkg -> pkg + versionSuffix).toList();
  }

  /**
   * @return {@link PackageManagerCommand} for installation.
   */
  public PackageManagerCommand install() {
    return this.packageManager.install(this);
  }

  /**
   * @return {@link PackageManagerCommand} for uninstallation.
   */
  public PackageManagerCommand uninstall() {
    return this.packageManager.uninstall(this);
  }
}
