package com.devonfw.tools.ide.tool.obsidian;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://obsidian.md/">Obsidian</a>.
 */
public class Obsidian extends GlobalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Obsidian(IdeContext context) {

    super(context, "obsidian", Set.of(Tag.MARK_DOWN));
  }

  @Override
  public boolean isExtract() {

    return switch (this.context.getSystemInfo().getOs()) {
      // the Windows download is an installer executable that has to be started, not extracted
      case WINDOWS -> false;
      // the macOS download is a DMG image that has to be mounted so the *.app can be taken out of it
      case MAC -> true;
      // TODO: depends on which Linux artifact the UrlUpdater ends up publishing (see #2186)
      case LINUX -> true;
    };
  }

  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {

    Path path = super.getInstallationPath(edition, resolvedVersion);
    if ((path == null) && this.context.getSystemInfo().isWindows()) {
      // the Obsidian installer does not add itself to the PATH so we have to find the installation via the registry
      return getInstallationPathFromWindowsRegistry();
    }
    return path;
  }

  private Path getInstallationPathFromWindowsRegistry() {

    WindowsAppInstallation installation = WindowsHelper.get(this.context).getAppInstallationFromRegistry(getWindowsRegistryAppName());
    if ((installation != null) && (installation.installLocation() != null)) {
      Path installationDir = Path.of(installation.installLocation());
      if (Files.isDirectory(installationDir)) {
        this.context.getPath().setPath(getName(), installationDir);
        return installationDir;
      }
    }
    return null;
  }

  @Override
  public String getWindowsRegistryAppName() {

    return "Obsidian";
  }
}
