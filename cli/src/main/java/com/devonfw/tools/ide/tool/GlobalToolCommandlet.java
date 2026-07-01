package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} that is installed globally.
 */
public abstract class GlobalToolCommandlet extends ToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalToolCommandlet.class);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public GlobalToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  /**
   * Performs the installation or uninstallation of the {@link #getName() tool} via a package manager.
   *
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param commandStrings commandStrings The package manager command strings to execute.
   * @return {@code true} if installation or uninstallation succeeds with any of the package manager commands, {@code false} otherwise.
   */
  protected boolean runWithPackageManager(boolean silent, String... commandStrings) {

    List<PackageManagerCommand> pmCommands = Arrays.stream(commandStrings).map(PackageManagerCommand::of).toList();
    return runWithPackageManager(silent, pmCommands);
  }

  /**
   * Performs the installation or uninstallation of the {@link #getName() tool} via a package manager.
   *
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param pmCommands A list of {@link PackageManagerCommand} to be used for installation or uninstallation.
   * @return {@code true} if installation or uninstallation succeeds with any of the package manager commands, {@code false} otherwise.
   */
  protected boolean runWithPackageManager(boolean silent, List<PackageManagerCommand> pmCommands) {

    for (PackageManagerCommand pmCommand : pmCommands) {
      NativePackageManager packageManager = pmCommand.packageManager();
      Path packageManagerPath = this.context.getPath().findBinary(Path.of(packageManager.getBinaryName()));
      if (packageManagerPath == null || !Files.exists(packageManagerPath)) {
        LOG.debug("{} is not installed", packageManager.toString());
        continue; // Skip to the next package manager command
      }

      if (executePackageManagerCommand(pmCommand, silent)) {
        return true; // Success
      }
    }
    return false; // None of the package manager commands were successful
  }

  /**
   * Logs the privileged commands before execution so the user knows why sudo/root permissions are requested.
   *
   * @param commands the privileged commands to log.
   */
  protected void logPrivilegedCommands(List<String> commands) {

    IdeLogLevel level = IdeLogLevel.INTERACTION;
    level.log(LOG, "We need to run the following privileged command(s):");
    for (String command : commands) {
      level.log(LOG, command);
    }
    level.log(LOG, "This will require root permissions!");
  }

  private void logPackageManagerCommands(PackageManagerCommand pmCommand) {

    logPrivilegedCommands(pmCommand.commands());
  }

  /**
   * Executes the provided package manager command.
   *
   * @param pmCommand The {@link PackageManagerCommand} containing the commands to execute.
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the package manager commands execute successfully, {@code false} otherwise.
   */
  private boolean executePackageManagerCommand(PackageManagerCommand pmCommand, boolean silent) {

    String bashPath = this.context.findBashRequired().toString();
    logPackageManagerCommands(pmCommand);
    for (String command : pmCommand.commands()) {
      ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable(bashPath)
          .addArgs("-c", command);
      int exitCode = pc.run();
      if (exitCode != 0) {
        LOG.warn("{} command did not execute successfully", command);
        return false;
      }
    }

    if (!silent) {
      IdeLogLevel.SUCCESS.log(LOG, "Successfully installed {}", this.tool);
    }
    return true;
  }

  @Override
  protected boolean isExtract() {

    // for global tools we usually download installers and do not want to extract them (e.g. installer.msi file shall
    // not be extracted)
    return false;
  }

  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    if (this.context.getSystemInfo().isLinux()) {
      // on Linux global tools are typically installed via the package manager of the OS
      // if a global tool implements this method to return at least one PackageManagerCommand, then we install this way.
      List<PackageManagerCommand> commands = getInstallPackageManagerCommands();
      if (!commands.isEmpty()) {
        boolean newInstallation = runWithPackageManager(request.isSilent(), commands);
        Path rootDir = getInstallationPath(getConfiguredEdition(), resolvedVersion);
        return createToolInstallation(rootDir, resolvedVersion, newInstallation, request.getProcessContext(), request.isAdditionalInstallation());
      }
    }

    ToolEdition toolEdition = getToolWithConfiguredEdition();
    Path installationPath = getInstallationPath(toolEdition.edition(), resolvedVersion);
    // if force mode is enabled, go through with the installation even if the tool is already installed
    if ((installationPath != null) && !this.context.isForceMode()) {
      return toolAlreadyInstalled(request);
    }
    String edition = toolEdition.edition();
    ToolRepository toolRepository = this.context.getDefaultToolRepository();
    resolvedVersion = cveCheck(request);
    // download and install the global tool
    Path target = toolRepository.download(this.tool, edition, resolvedVersion, this);
    ProcessContext pc;
    if (isMacDmg(target)) {
      installMacDmg(target);
      pc = request.getProcessContext();
    } else {
      FileAccess fileAccess = this.context.getFileAccess();
      Path executable = target;
      Path tmpDir = null;
      boolean extract = isExtract();
      if (extract) {
        tmpDir = fileAccess.createTempDir(getName());
        Path downloadBinaryPath = tmpDir.resolve(target.getFileName());
        fileAccess.extract(target, downloadBinaryPath);
        executable = fileAccess.findFirst(downloadBinaryPath, Files::isExecutable, false);
      }
      pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable(executable);
      int exitCode = pc.run(ProcessMode.BACKGROUND_SILENT).getExitCode();
      if (tmpDir != null) {
        fileAccess.delete(tmpDir);
      }
      if (exitCode != 0) {
        throw new CliException("Installation process for " + this.tool + " in version " + resolvedVersion + " failed with exit code " + exitCode + "!");
      }
    }
    IdeLogLevel.SUCCESS.log(LOG, "Installation process for {} in version {} has started", this.tool, resolvedVersion);
    Step step = request.getStep();
    if (step != null) {
      step.success(true);
    }
    installationPath = getInstallationPath(toolEdition.edition(), resolvedVersion);
    if (installationPath == null) {
      throw new CliException("The tool " + this.tool + " is about to be installed. Please complete the installation and if required "
          + "reboot your machine. Then rerun the command to start the tool.", 2);
    }
    return createToolInstallation(installationPath, resolvedVersion, true, pc, false);
  }

  private void installMacDmg(Path downloadedToolFile) {

    FileAccess fileAccess = this.context.getFileAccess();
    Path tmpDir = fileAccess.createTempDir(getName());
    try {
      fileAccess.extractDmg(downloadedToolFile, tmpDir);
      Path sourceApp = getMacOsHelper().findAppDir(tmpDir);
      if (sourceApp == null) {
        throw new CliException("Failed to install " + this.tool + " from " + downloadedToolFile + " because no MacOS *.app was found.");
      }
      Path targetApp = getMacApplicationsPath().resolve(sourceApp.getFileName().toString());
      copyMacApplicationToApplications(sourceApp, targetApp);
    } finally {
      fileAccess.delete(tmpDir);
    }
  }

  /**
   * Copies a macOS application bundle to the global applications folder.
   *
   * @param sourceApp the extracted source {@code .app}.
   * @param targetApp the target {@code .app} in {@link #getMacApplicationsPath()}.
   */
  protected void copyMacApplicationToApplications(Path sourceApp, Path targetApp) {

    runPrivilegedCommands(List.of(
        List.of("/bin/rm", "-rf", targetApp.toString()),
        List.of("/usr/bin/ditto", sourceApp.toString(), targetApp.toString())));
  }

  private void runPrivilegedCommands(List<List<String>> commands) {

    logPrivilegedCommands(commands.stream().map(this::toSudoCommandLine).toList());
    for (List<String> command : commands) {
      int exitCode = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable("sudo").addArgs(command).run();
      if (exitCode != 0) {
        throw new CliException("Privileged command failed with exit code " + exitCode + ": " + toSudoCommandLine(command));
      }
    }
  }

  private String toSudoCommandLine(List<String> command) {

    return "sudo " + String.join(" ", command);
  }

  private boolean isMacDmg(Path file) {

    if (!this.context.getSystemInfo().isMac()) {
      return false;
    }
    String extension = FilenameUtil.getExtension(file.toString());
    return "dmg".equals(extension);
  }

  /**
   * @return the macOS applications folder where global {@code .dmg} tools are installed.
   */
  protected Path getMacApplicationsPath() {

    return Path.of("/Applications");
  }

  /**
   * @return the {@link List} of {@link PackageManagerCommand}s to use on Linux to install this tool. If empty, no package manager installation will be
   *     triggered on Linux.
   */
  protected List<PackageManagerCommand> getInstallPackageManagerCommands() {
    return List.of();
  }

  @Override
  public VersionIdentifier getInstalledVersion() {
    //TODO: handle "get-version <globaltool>"
    return null;
  }

  @Override
  public String getInstalledEdition() {
    //TODO: handle "get-edition <globaltool>"
    return null;
  }

  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {

    Path toolBinary = Path.of(getBinaryName());
    Path binaryPath = this.context.getPath().findBinary(toolBinary);
    if ((binaryPath == toolBinary) || !Files.exists(binaryPath)) {
      if (this.context.getSystemInfo().isMac()) {
        return getMacApplicationInstallationPath();
      }
      return null;
    }
    Path binPath = binaryPath.getParent();
    if (binPath == null) {
      return null;
    }
    return this.context.getFileAccess().getBinParentPath(binPath);
  }

  private Path getMacApplicationInstallationPath() {

    Path appPath = this.context.getFileAccess().findFirst(getMacApplicationsPath(), this::isMacApplicationForTool, false);
    if (appPath == null) {
      return null;
    }
    Path binaryPath = getMacApplicationBinaryPath(appPath);
    this.context.getPath().setPath(getName(), binaryPath.getParent());
    return appPath;
  }

  private boolean isMacApplicationForTool(Path appPath) {

    if (!Files.isDirectory(appPath) || !appPath.getFileName().toString().endsWith(".app")) {
      return false;
    }
    return Files.isExecutable(getMacApplicationBinaryPath(appPath));
  }

  private Path getMacApplicationBinaryPath(Path appPath) {

    return appPath.resolve(IdeContext.FOLDER_CONTENTS).resolve(IdeContext.FOLDER_MAC_OS).resolve(getBinaryName());
  }

  @Override
  public void uninstall() {
    //TODO: handle "uninstall <globaltool>"
    LOG.error("Couldn't uninstall " + this.getName());
  }
}
