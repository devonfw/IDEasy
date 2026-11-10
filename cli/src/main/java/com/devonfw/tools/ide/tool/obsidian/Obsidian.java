package com.devonfw.tools.ide.tool.obsidian;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.EditionAndVersion;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://obsidian.md/">Obsidian</a>.
 * <p>
 * On Windows the downloaded installer is started. On macOS the *.app bundle is taken out of the DMG image and persisted in the Applications folder of
 * the user. On Linux the tar.gz archive is extracted and persisted in {@code ~/.local/share/obsidian}.
 */
public class Obsidian extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Obsidian.class);

  private static final String MAC_APPLICATION_NAME = "Obsidian";

  private static final String MAC_INFO_PLIST = "Info.plist";

  private static final String MAC_FOLDER_MACOS = "MacOS";

  private static final Pattern MAC_BUNDLE_VERSION_PATTERN = Pattern.compile("<key>CFBundleShortVersionString</key>\\s*<string>\\s*([^<\\s]+)\\s*</string>");

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
      // the Linux download is a tar.gz archive
      case LINUX -> true;
    };
  }

  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    if (this.context.getSystemInfo().isWindows()) {
      return super.doInstall(request);
    }
    VersionIdentifier resolvedVersion = cveCheck(request);
    if (request.isAlreadyInstalled() && !this.context.isForceMode()) {
      return toolAlreadyInstalled(request);
    }
    String edition = request.getRequested().getEdition().edition();
    Path download = this.context.getDefaultToolRepository().download(this.tool, edition, resolvedVersion, this);
    Path installationPath;
    if (this.context.getSystemInfo().isMac()) {
      installationPath = installMacAppBundle(download);
    } else {
      installationPath = installLinux(download, resolvedVersion);
    }
    IdeLogLevel.SUCCESS.log(LOG, "Successfully installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
    Step step = request.getStep();
    if (step != null) {
      step.success(true);
    }
    return createToolInstallation(installationPath, resolvedVersion, true, request.getProcessContext(), request.isAdditionalInstallation());
  }

  private Path installMacAppBundle(Path download) {

    FileAccess fileAccess = this.context.getFileAccess();
    Path tmpDir = fileAccess.createTempDir(getName());
    Path extractedDir = tmpDir.resolve(download.getFileName());
    fileAccess.extract(download, extractedDir);
    Path appBundle = getMacOsHelper().findAppDir(extractedDir);
    if (appBundle == null) {
      throw new CliException("Failed to install " + this.tool + " as no *.app bundle was found in " + download);
    }
    getMacOsHelper().removeQuarantineAttribute(extractedDir);
    Path targetBundle = getMacUserApplicationsPath().resolve(MAC_APPLICATION_NAME + ".app");
    fileAccess.mkdirs(targetBundle.getParent());
    fileAccess.delete(targetBundle);
    fileAccess.move(appBundle, targetBundle);
    fileAccess.delete(tmpDir);
    return targetBundle;
  }

  private Path installLinux(Path download, VersionIdentifier resolvedVersion) {

    FileAccess fileAccess = this.context.getFileAccess();
    Path installationPath = getLinuxInstallationPath();
    fileAccess.mkdirs(installationPath.getParent());
    fileAccess.delete(installationPath);
    fileAccess.extract(download, installationPath);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    return installationPath;
  }

  @Override
  protected EditionAndVersion computeInstalledEditionAndVersion() {

    if (this.context.getSystemInfo().isWindows()) {
      return super.computeInstalledEditionAndVersion();
    }
    VersionIdentifier version = this.context.getSystemInfo().isMac() ? getMacInstalledVersion() : getLinuxInstalledVersion();
    return (version != null) ? new EditionAndVersion(this.tool, version) : null;
  }

  private VersionIdentifier getMacInstalledVersion() {

    Path appBundle = findMacApplicationBundle();
    if (appBundle == null) {
      return null;
    }
    Path infoPlist = appBundle.resolve(IdeContext.FOLDER_CONTENTS).resolve(MAC_INFO_PLIST);
    String content = this.context.getFileAccess().readFileContent(infoPlist);
    if (content == null) {
      return null;
    }
    VersionIdentifier version = resolveVersionWithPattern(content, MAC_BUNDLE_VERSION_PATTERN);
    if (version == null) {
      LOG.warn("Could not determine the installed version of {} from {}", this.tool, infoPlist);
    }
    return version;
  }

  private VersionIdentifier getLinuxInstalledVersion() {

    String version = this.context.getFileAccess().readFileContent(getLinuxInstallationPath().resolve(IdeContext.FILE_SOFTWARE_VERSION));
    if ((version == null) || version.isBlank()) {
      return null;
    }
    return VersionIdentifier.of(version.trim());
  }

  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {

    return switch (this.context.getSystemInfo().getOs()) {
      case WINDOWS -> {
        Path path = super.getInstallationPath(edition, resolvedVersion);
        // the Obsidian installer does not add itself to the PATH so we have to find the installation via the registry
        yield (path == null) ? getInstallationPathFromWindowsRegistry() : path;
      }
      case MAC -> findMacApplicationBundle();
      case LINUX -> {
        Path installationPath = getLinuxInstallationPath();
        yield Files.isDirectory(installationPath) ? installationPath : null;
      }
    };
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
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode) {

    Path binary = getPersistedBinary();
    if ((binary != null) && Files.exists(binary)) {
      pc.executable(binary);
    } else {
      super.configureToolBinary(pc, processMode);
    }
  }

  /**
   * @return the {@link Path} to the binary of the installation persisted by IDEasy on macOS or Linux, or {@code null} if not applicable.
   */
  private Path getPersistedBinary() {

    return switch (this.context.getSystemInfo().getOs()) {
      case WINDOWS -> null;
      case MAC -> {
        Path appBundle = findMacApplicationBundle();
        yield (appBundle == null) ? null : appBundle.resolve(IdeContext.FOLDER_CONTENTS).resolve(MAC_FOLDER_MACOS).resolve(MAC_APPLICATION_NAME);
      }
      case LINUX -> getLinuxInstallationPath().resolve(getBinaryName());
    };
  }

  /**
   * @return the {@link Path} where Obsidian gets installed on Linux.
   */
  private Path getLinuxInstallationPath() {

    return this.context.getUserHome().resolve(".local").resolve("share").resolve(this.tool);
  }

  @Override
  public void uninstall() {

    if (!this.context.getSystemInfo().isLinux()) {
      super.uninstall();
      return;
    }
    Path installationPath = getLinuxInstallationPath();
    if (Files.isDirectory(installationPath)) {
      this.context.getFileAccess().delete(installationPath);
      invalidateInstalledEditionAndVersion();
      IdeLogLevel.SUCCESS.log(LOG, "Successfully uninstalled {} by removing {}", this.tool, installationPath);
    } else {
      LOG.warn("Couldn't uninstall {} as it is not installed at {}. Please uninstall it manually.", this.tool, installationPath);
    }
  }

  @Override
  public String getMacApplicationName() {

    return MAC_APPLICATION_NAME;
  }

  @Override
  public String getWindowsRegistryAppName() {

    return "Obsidian";
  }
}
