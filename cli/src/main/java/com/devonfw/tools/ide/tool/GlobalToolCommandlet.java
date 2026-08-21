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
import com.devonfw.tools.ide.os.WindowsAppInstallation;
import com.devonfw.tools.ide.os.WindowsHelper;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
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
   * @param action the {@link NativePackageAction} that is performed - only used for logging.
   * @return {@code true} if installation or uninstallation succeeds with any of the package manager commands, {@code false} otherwise.
   */
  protected boolean runWithPackageManager(boolean silent, NativePackageAction action, String... commandStrings) {

    List<PackageManagerCommand> pmCommands = Arrays.stream(commandStrings).map(PackageManagerCommand::of).toList();
    return runWithPackageManager(silent, pmCommands, action);
  }

  /**
   * Performs the installation or uninstallation of the {@link #getName() tool} via a package manager.
   *
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param pmCommands A list of {@link PackageManagerCommand} to be used for installation or uninstallation.
   * @param action the {@link NativePackageAction} that is performed - only used for logging.
   * @return {@code true} if installation or uninstallation succeeds with any of the package manager commands, {@code false} otherwise.
   */
  protected boolean runWithPackageManager(boolean silent, List<PackageManagerCommand> pmCommands, NativePackageAction action) {

    for (PackageManagerCommand pmCommand : pmCommands) {
      NativePackageManager packageManager = pmCommand.packageManager();
      if (!isPackageManagerAvailable(packageManager)) {
        LOG.debug("{} is not installed", packageManager);
        continue; // Skip to the next package manager command
      }

      if (executePackageManagerCommand(pmCommand, silent, action)) {
        return true; // Success
      }
    }
    return false; // None of the package manager commands were successful
  }

  private void logPackageManagerCommands(PackageManagerCommand pmCommand) {

    IdeLogLevel level = IdeLogLevel.INTERACTION;
    level.log(LOG, "We need to run the following privileged command(s):");
    for (String command : pmCommand.commands()) {
      level.log(LOG, command);
    }
    level.log(LOG, "This will require root permissions!");
  }

  /**
   * Executes the provided package manager command.
   *
   * @param pmCommand The {@link PackageManagerCommand} containing the commands to execute.
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param action the {@link NativePackageAction} tht is performed - only used for logging.
   * @return {@code true} if the package manager commands execute successfully, {@code false} otherwise.
   */
  private boolean executePackageManagerCommand(PackageManagerCommand pmCommand, boolean silent, NativePackageAction action) {

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
      IdeLogLevel.SUCCESS.log(LOG, "Successfully {} {}", action.getAction(), this.tool);
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
      // if a global tool implements getNativePackages() to returns at least one NativePackage, then we will install this way.
      List<PackageManagerCommand> commands = getInstallPackageManagerCommands(resolvedVersion);
      if (!commands.isEmpty()) {
        boolean newInstallation = runWithPackageManager(request.isSilent(), commands, NativePackageAction.INSTALL);
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
    FileAccess fileAccess = this.context.getFileAccess();
    Path target = toolRepository.download(this.tool, edition, resolvedVersion, this);
    Path executable = target;
    Path tmpDir = null;
    boolean extract = isExtract();
    if (extract) {
      tmpDir = fileAccess.createTempDir(getName());
      Path downloadBinaryPath = tmpDir.resolve(target.getFileName());
      fileAccess.extract(target, downloadBinaryPath);
      executable = fileAccess.findFirst(downloadBinaryPath, Files::isExecutable, false);
    }
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable(executable);
    int exitCode = pc.run(ProcessMode.BACKGROUND_SILENT).getExitCode();
    if (tmpDir != null) {
      fileAccess.delete(tmpDir);
    }
    if (exitCode == 0) {
      IdeLogLevel.SUCCESS.log(LOG, "Installation process for {} in version {} has started", this.tool, resolvedVersion);
      Step step = request.getStep();
      if (step != null) {
        step.success(true);
      }
    } else {
      throw new CliException("Installation process for " + this.tool + " in version " + resolvedVersion + " failed with exit code " + exitCode + "!");
    }
    installationPath = getInstallationPath(toolEdition.edition(), resolvedVersion);
    if (installationPath == null) {
      return new ToolInstallation(null, null, null, resolvedVersion, true, true);
    }
    return createToolInstallation(installationPath, resolvedVersion, true, pc, false);
  }

  /**
   * @return the {@link List} of {@link NativePackage}s this tool consists of on Linux - one per supported {@link NativePackageManager}. Override this method
   *     instead of {@link #getInstallPackageManagerCommands} so that the commands for installation and uninstallation can both be derived from this
   *     declaration. If empty, no package manager installation will be triggered on Linux.
   */
  protected List<NativePackage> getNativePackages() {
    return List.of();
  }

  /**
   * @return the {@link List} of {@link PackageManagerCommand}s to use on Linux to install this tool. If empty, no package manager installation will be
   *     triggered on Linux.
   */
  protected List<PackageManagerCommand> getInstallPackageManagerCommands(VersionIdentifier resolvedVersion) {
    String version = (resolvedVersion == null) ? null : resolvedVersion.toString();
    return getNativePackages().stream().map(nativePackage -> nativePackage.install(version)).toList();
  }

  /**
   * @return the {@link List} of {@link PackageManagerCommand}s to use on Linux to uninstall this tool. If empty, no package manager uninstallation will be
   *     triggered on Linux.
   */
  protected List<PackageManagerCommand> getUninstallPackageManagerCommands() {
    return getNativePackages().stream().map(NativePackage::uninstall).toList();
  }

  /**
   * @param packageManager the {@link NativePackageManager} to check.
   * @return {@code true} if the given {@link NativePackageManager} is available on the current system, {@code false} otherwise.
   */
  protected boolean isPackageManagerAvailable(NativePackageManager packageManager) {
    Path binary = Path.of(packageManager.getBinaryName());
    Path binaryPath = this.context.getPath().findBinary(binary);
    return (binaryPath != binary) && Files.exists(binaryPath);
  }

  /**
   * @param nativePackage the {@link NativePackage} to query.
   * @return the raw version reported by the {@link NativePackageManager} or {@code null} if the package is not installed.
   */
  protected String queryNativePackageVersion(NativePackage nativePackage) {
    List<String> command = nativePackage.getVersionQueryCommand();
    String[] args = command.subList(1, command.size()).toArray(String[]::new);
    ProcessResult result = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).executable(command.getFirst()).addArgs(args)
        .run(ProcessMode.DEFAULT_CAPTURE);
    if (!result.isSuccessful()) {
      return null;
    }
    return nativePackage.getPackageManager().parseVersionQueryOutput(result.getSingleOutput(IdeLogLevel.DEBUG));
  }

  /**
   * @return the {@link VersionIdentifier} of this tool as reported by the OS native package manager it was installed with or {@code null} if this tool is not
   *     installed via any of its {@link #getNativePackages() native packages}.
   */
  protected VersionIdentifier getNativePackageVersion() {
    for (NativePackage nativePackage : getNativePackages()) {
      if (!isPackageManagerAvailable(nativePackage.getPackageManager())) {
        continue;
      }
      String version = queryNativePackageVersion(nativePackage);
      if ((version != null) && !version.isBlank()) {
        return VersionIdentifier.of(version.trim());
      }
    }
    return null;
  }

  /**
   * @return the app name to look for in the Windows registry
   */
  public String getWindowsRegistryAppName() {

    return this.tool;
  }

  @Override
  public VersionIdentifier getInstalledVersion() {
    if (this.context.getSystemInfo().isLinux()) {
      return getNativePackageVersion();
    }
    if (this.context.getSystemInfo().isWindows()) {
      WindowsAppInstallation installation = WindowsHelper.get(this.context).getAppInstallationFromRegistry(getWindowsRegistryAppName());
      if (installation != null) {
        return VersionIdentifier.of(installation.version());
      }
    }
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
      return null;
    }
    Path binPath = binaryPath.getParent();
    if (binPath == null) {
      return null;
    }
    return this.context.getFileAccess().getBinParentPath(binPath);
  }

  @Override
  public void uninstall() {
    if (this.context.getSystemInfo().isLinux()) {
      runWithPackageManager(false, getUninstallPackageManagerCommands(), NativePackageAction.UNINSTALL);
    } else {
      LOG.error("Couldn't uninstall {} on this OS. Please uninstall manually.", this.getName());
    }
  }
}
