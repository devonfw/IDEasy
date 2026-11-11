package com.devonfw.tools.ide.tool.git;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.ini.IniFile;
import com.devonfw.tools.ide.io.ini.IniSection;
import com.devonfw.tools.ide.tool.GlobalToolCommandlet;
import com.devonfw.tools.ide.tool.NativePackage;
import com.devonfw.tools.ide.tool.NativePackageAction;
import com.devonfw.tools.ide.tool.NativePackageManager;
import com.devonfw.tools.ide.tool.PackageManagerCommand;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GlobalToolCommandlet} for <a href="https://git-scm.com/">Git</a>.
 */
public class Git extends GlobalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Git.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Git(IdeContext context) {
    super(context, "git", Set.of(Tag.GIT));
  }

  /**
   * @return {@code false} since Git is required to create a project and can therefore also be installed outside of a project.
   */
  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  protected List<NativePackage> getNativePackages() {
    return List.of(
        NativePackage.of(NativePackageManager.APT, "git"),
        NativePackage.of(NativePackageManager.ZYPPER, "git"),
        NativePackage.of(NativePackageManager.BREW, "git"),
        NativePackage.of(NativePackageManager.WINGET, "Git.Git"));
  }

  @Override
  public String getWindowsRegistryAppName() {
    return "Git";
  }

  /**
   * @return {@code true} as Git is installed via the OS native package manager on all operating systems. So no URL metadata is required that could only be
   *     cloned with Git.
   */
  @Override
  protected boolean isPackageManagerBasedInstallation() {
    return true;
  }

  @Override
  protected List<PackageManagerCommand> getInstallPackageManagerCommands(VersionIdentifier resolvedVersion) {

    return super.getInstallPackageManagerCommands(resolvedVersion).stream().map(this::addWingetOptions).toList();
  }

  /**
   * The first installation shows the installation wizard and winget may ask for agreements so nothing is accepted on behalf of the user. Upgrades are silent.
   * In batch mode nobody can answer, so also the agreement of the source (not of Git itself) is accepted.
   */
  private PackageManagerCommand addWingetOptions(PackageManagerCommand command) {

    if (command.packageManager() != NativePackageManager.WINGET) {
      return command;
    }
    String options;
    if (this.context.isBatchMode()) {
      options = " --silent --accept-source-agreements --disable-interactivity";
    } else if (getInstalledVersion() != null) {
      options = " --silent";
    } else {
      options = " --interactive";
    }
    return new PackageManagerCommand(command.packageManager(), command.commands().stream().map(commandLine -> commandLine + options).toList());
  }

  /**
   * Like on Linux and macOS the package manager is run on every call so that Git gets upgraded if a newer version is available. Only a Git that is found on
   * the PATH without being known to the registry (e.g. PortableGit or Scoop) is used as is, since winget cannot manage it and would install a second Git. An
   * installed Git is only available in a new terminal, as the installer cannot change the PATH of the current one.
   */
  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    if (!this.context.getSystemInfo().isWindows()) {
      return super.doInstall(request);
    }
    VersionIdentifier previousVersion = getInstalledVersion();
    Path rootDir = getInstallationPath(getConfiguredEdition(), previousVersion);
    if ((previousVersion == null) && (rootDir != null) && !this.context.isForceMode()) {
      logToolAlreadyInstalled(request);
      return createToolInstallation(rootDir, null, false, request.getProcessContext(), request.isAdditionalInstallation());
    }
    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    boolean success = runWithPackageManager(request.isSilent(), getInstallPackageManagerCommands(resolvedVersion), NativePackageAction.INSTALL);
    if (success) {
      invalidateInstalledEditionAndVersion();
      enableLongpaths();
    } else if (previousVersion == null) {
      throw new CliException("Failed to install Git via winget, it may not be installed or may be disabled by your IT administrators.\n"
          + IdeContext.PLEASE_DOWNLOAD_AND_INSTALL_GIT + ":\n " + IdeContext.WINDOWS_GIT_DOWNLOAD_URL);
    } else {
      LOG.warn("Failed to install Git via winget, continuing with the installed version {}.", previousVersion);
    }
    VersionIdentifier installedVersion = getInstalledVersion();
    rootDir = getInstallationPath(getConfiguredEdition(), installedVersion);
    if (rootDir == null) {
      if (previousVersion == null) {
        return new ToolInstallation(null, null, null, installedVersion, true, true);
      }
      throw new CliException("Git " + installedVersion + " is installed but not available on your PATH. Please open a new terminal. If this does not "
          + "help, reinstall Git with the option to use it from the command line.");
    }
    return createToolInstallation(rootDir, installedVersion, success, request.getProcessContext(), request.isAdditionalInstallation());
  }

  /**
   * Enables long paths in the global git configuration as Git for Windows is limited to 260 characters per path by default.
   */
  public void enableLongpaths() {

    Path configPath = this.context.getUserHome().resolve(".gitconfig");
    FileAccess fileAccess = this.context.getFileAccess();
    IniFile iniFile = fileAccess.readIniFile(configPath);
    IniSection coreSection = iniFile.getOrCreateSection("core");
    if (!coreSection.getPropertyKeys().contains("longpaths") || !"true".equals(coreSection.getPropertyValue("longpaths"))) {
      coreSection.setProperty("longpaths", "true");
      fileAccess.writeIniFile(iniFile, configPath);
    }
  }
}
