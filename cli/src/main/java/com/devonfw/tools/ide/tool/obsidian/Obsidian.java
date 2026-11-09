package com.devonfw.tools.ide.tool.obsidian;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(Obsidian.class);

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
    if (installation == null) {
      return null;
    }
    // Installers like NSIS only write InstallLocation to a separate key, not to the uninstall key we read, but they register their executables
    // (DisplayIcon, UninstallString) that live in the installation folder. So we try InstallLocation first and then these as fallback.
    List<Path> candidates = new ArrayList<>();
    if (installation.installLocation() != null) {
      addPath(candidates, installation.installLocation());
    }
    candidates.add(getFolderOfExecutable(installation.icon()));
    candidates.add(getFolderOfExecutable(installation.uninstallString()));
    for (Path installationDir : candidates) {
      if ((installationDir != null) && Files.isDirectory(installationDir)) {
        this.context.getPath().setPath(getName(), installationDir);
        return installationDir;
      }
    }
    LOG.debug("{} found in the registry but without a usable installation folder (InstallLocation={}, DisplayIcon={}, UninstallString={}).", getName(),
        installation.installLocation(), installation.icon(), installation.uninstallString());
    return null;
  }

  private static void addPath(List<Path> paths, String path) {

    try {
      paths.add(Path.of(path));
    } catch (InvalidPathException e) {
      LOG.debug("Ignoring invalid path '{}' from the registry.", path);
    }
  }

  /**
   * @param command a command line or icon location as stored in the Windows registry, e.g. {@code "C:/Program Files/App/uninstall.exe" /S} or
   *     {@code C:/Users/Me/App/app.exe,0}.
   * @return the parent folder of the executable in the given command or {@code null} if it could not be determined.
   */
  static Path getFolderOfExecutable(String command) {

    if (command == null) {
      return null;
    }
    String cmd = command.trim();
    String executable;
    if (cmd.startsWith("\"")) {
      int end = cmd.indexOf('"', 1);
      if (end < 0) {
        return null;
      }
      executable = cmd.substring(1, end);
    } else {
      // not quoted: the path may contain spaces, so we cut after the first ".exe" instead of splitting at whitespace
      int end = cmd.toLowerCase(Locale.ROOT).indexOf(".exe");
      if (end >= 0) {
        executable = cmd.substring(0, end + ".exe".length());
      } else {
        int comma = cmd.lastIndexOf(',');
        executable = (comma > 0) ? cmd.substring(0, comma) : cmd;
      }
    }
    try {
      return Path.of(executable).getParent();
    } catch (InvalidPathException e) {
      return null;
    }
  }

  @Override
  public String getWindowsRegistryAppName() {

    return "Obsidian";
  }
}
