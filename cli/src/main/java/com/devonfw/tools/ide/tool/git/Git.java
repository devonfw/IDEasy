package com.devonfw.tools.ide.tool.git;

import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageManager;

/**
 * {@link GlobalToolCommandlet} for <a href="https://git-scm.com/">Git</a>.
 */
public class Git extends GlobalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Git(IdeContext context) {
    super(context, "git", Set.of(Tag.GIT));
  }

  @Override
  protected String getBinaryName() {
    return "git";
  }

  @Override
  protected List<NativePackage> getNativePackages() {
    return List.of(
        NativePackage.of(NativePackageManager.APT, "git"),
        NativePackage.of(NativePackageManager.ZYPPER, "git"),
        NativePackage.of(NativePackageManager.BREW, "git"));
  }

  @Override
  public String getWindowsRegistryAppName() {
    return "Git";
  }

  @Override
  protected List<String> getInstallerArguments() {
    if (this.context.getSystemInfo().isWindows()) {
      return List.of(
          "/VERYSILENT",
          "/NORESTART",
          "/NOCANCEL",
          "/SP-"
      );
    }
    return List.of();
  }

  /**
   * On macOS Git is installed via Homebrew (see {@link #getNativePackages()}) rather than via the DMG installer artifact, so the package manager based
   * installation flow is used there too - exactly like on Linux. On Windows the standard installer artifact is still used.
   *
   * @return {@code true} if Git is installed via the OS native package manager.
   */
  @Override
  protected boolean isPackageManagerBasedInstallation() {
    return this.context.getSystemInfo().isLinux() || this.context.getSystemInfo().isMac();
  }
}
