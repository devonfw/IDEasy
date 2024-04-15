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
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.repo.ToolRepository;
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
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   * method.
   */
  public GlobalToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  /**
   * Performs the installation of the {@link #getName() tool} via a package manager.
   *
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param commandStrings commandStrings The package manager command strings to execute.
   * @return {@code true} if installation succeeds with any of the package manager commands, {@code false} otherwise.
   */
  protected boolean installWithPackageManager(boolean silent, String... commandStrings) {

    List<PackageManagerCommand> pmCommands = Arrays.stream(commandStrings).map(PackageManagerCommand::of).toList();
    return installWithPackageManager(silent, pmCommands);
  }

  /**
   * Performs the installation of the {@link #getName() tool} via a package manager.
   * 
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param pmCommands A list of {@link PackageManagerCommand} to be used for installation.
   * @return {@code true} if installation succeeds with any of the package manager commands, {@code false} otherwise.
   */
  protected boolean installWithPackageManager(boolean silent, List<PackageManagerCommand> pmCommands) {

    for (PackageManagerCommand pmCommand : pmCommands) {
      PackageManager packageManager = pmCommand.packageManager();
      Path packageManagerPath = this.context.getPath().findBinary(Path.of(packageManager.getBinaryName()));
      if (packageManagerPath == null || !Files.exists(packageManagerPath)) {
        this.context.debug("{} is not installed", packageManager.toString());
        continue; // Skip to the next package manager command
      }

      if (executePackageManagerCommand(pmCommand, silent)) {
        return true; // Successfully installed
      }
    }
    return false; // None of the package manager commands were successful
  }

  /**
   * Executes the provided package manager command.
   *
   * @param pmCommand The {@link PackageManagerCommand} containing the commands to execute.
   * @param silent {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the package manager commands execute successfully, {@code false} otherwise.
   */
  private boolean executePackageManagerCommand(PackageManagerCommand pmCommand, boolean silent) {

    String bashPath = this.context.findBash();
    for (String command : pmCommand.commands()) {
      ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(bashPath)
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
  protected boolean doInstall(boolean silent) {

    Path binaryPath = this.context.getPath().findBinary(Path.of(getBinaryName()));
    // if force mode is enabled, go through with the installation even if the tool is already installed
    if (binaryPath != null && Files.exists(binaryPath) && !this.context.isForceMode()) {
      IdeLogLevel level = silent ? IdeLogLevel.DEBUG : IdeLogLevel.INFO;
      this.context.level(level).log("{} is already installed at {}", this.tool, binaryPath);
      return false;
    }
    String edition = getEdition();
    ToolRepository toolRepository = this.context.getDefaultToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, configuredVersion);
    // download and install the global tool
    FileAccess fileAccess = this.context.getFileAccess();
    Path target = toolRepository.download(this.tool, edition, resolvedVersion);
    Path executable = target;
    Path tmpDir = null;
    boolean extract = isExtract();
    if (extract) {
      tmpDir = fileAccess.createTempDir(getName());
      Path downloadBinaryPath = tmpDir.resolve(target.getFileName());
      fileAccess.extract(target, downloadBinaryPath);
      executable = fileAccess.findFirst(downloadBinaryPath, Files::isExecutable, false);
    }
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(executable);
    int exitCode = pc.run();
    if (tmpDir != null) {
      fileAccess.delete(tmpDir);
    }
    if (exitCode == 0) {
      this.context.success("Successfully installed {} in version {}", this.tool, resolvedVersion);
    } else {
      this.context.warning("{} in version {} was not successfully installed", this.tool, resolvedVersion);
      return false;
    }
    postInstall();
    return true;
  }
}
