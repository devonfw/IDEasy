package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} that is installed globally.
 */
public abstract class GlobalToolCommandlet extends ToolCommandlet {

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
      PackageManager packageManager = pmCommand.packageManager();
      Path packageManagerPath = this.context.getPath().findBinary(Path.of(packageManager.getBinaryName()));
      if (packageManagerPath == null || !Files.exists(packageManagerPath)) {
        this.context.debug("{} is not installed", packageManager.toString());
        continue; // Skip to the next package manager command
      }

      if (executePackageManagerCommand(pmCommand, silent)) {
        return true; // Success
      }
    }
    return false; // None of the package manager commands were successful
  }

  private void logPackageManagerCommands(PackageManagerCommand pmCommand) {

    this.context.interaction("We need to run the following privileged command(s):");
    for (String command : pmCommand.commands()) {
      this.context.interaction(command);
    }
    this.context.interaction("This will require root permissions!");
  }

  /**
   * Executes the provided package manager command.
   *
   * @param pmCommand The {@link PackageManagerCommand} containing the commands to execute.
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the package manager commands execute successfully, {@code false} otherwise.
   */
  private boolean executePackageManagerCommand(PackageManagerCommand pmCommand, boolean silent) {

    String bashPath = this.context.findBashRequired();
    logPackageManagerCommands(pmCommand);
    for (String command : pmCommand.commands()) {
      ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING).executable(bashPath)
          .addArgs("-c", command);
      int exitCode = pc.run();
      if (exitCode != 0) {
        this.context.warning("{} command did not execute successfully", command);
        return false;
      }
    }

    if (!silent) {
      this.context.success("Successfully installed {}", this.tool);
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
  public boolean install(boolean silent, EnvironmentContext environmentContext) {

    Path binaryPath = this.context.getPath().findBinary(Path.of(getBinaryName()));
    // if force mode is enabled, go through with the installation even if the tool is already installed
    if (binaryPath != null && Files.exists(binaryPath) && !this.context.isForceMode()) {
      IdeLogLevel level = silent ? IdeLogLevel.DEBUG : IdeLogLevel.INFO;
      this.context.level(level).log("{} is already installed at {}", this.tool, binaryPath);
      return false;
    }
    String edition = getConfiguredEdition();
    ToolRepository toolRepository = this.context.getDefaultToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, configuredVersion, this);
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
    int exitCode = pc.run(ProcessMode.BACKGROUND).getExitCode();
    if (tmpDir != null) {
      fileAccess.delete(tmpDir);
    }
    if (exitCode == 0) {
      this.context.success("Installation process for {} in version {} has started", this.tool, resolvedVersion);
    } else {
      this.context.warning("{} in version {} was not successfully installed", this.tool, resolvedVersion);
      return false;
    }
    return true;
  }

  @Override
  public VersionIdentifier getInstalledVersion() {
    //TODO: handle "get-version <globaltool>"
    this.context.error("Couldn't get installed version of " + this.getName());
    return null;
  }

  @Override
  public String getInstalledEdition() {
    //TODO: handle "get-edition <globaltool>"
    this.context.error("Couldn't get installed edition of " + this.getName());
    return null;
  }

  @Override
  public void uninstall() {
    //TODO: handle "uninstall <globaltool>"
    this.context.error("Couldn't uninstall " + this.getName());
  }
}
