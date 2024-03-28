package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
   *        method.
   */
  public GlobalToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  /**
   * Performs the installation of the {@link #getName() tool} via a package manager.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param commands - A {@link Map} containing the commands used to perform the installation for each package manager.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and
   *         nothing has changed.
   */
  protected boolean installWithPackageManger(Map<PackageManager, List<String>> commands, boolean silent) {

    Path binaryPath = this.context.getPath().findBinary(Path.of(getBinaryName()));

    if (binaryPath != null && Files.exists(binaryPath) && !this.context.isForceMode()) {
      IdeLogLevel level = silent ? IdeLogLevel.DEBUG : IdeLogLevel.INFO;
      this.context.level(level).log("{} is already installed at {}", this.tool, binaryPath);
      return false;
    }

    Path bashPath = this.context.getPath().findBinary(Path.of("bash"));
    if (bashPath == null || !Files.exists(bashPath)) {
      context.warning("Bash was not found on this machine. Not Proceeding with installation of tool " + this.tool);
      return false;
    }

    PackageManager foundPackageManager = null;
    for (PackageManager pm : commands.keySet()) {
      if (Files.exists(this.context.getPath().findBinary(Path.of(pm.toString().toLowerCase())))) {
        foundPackageManager = pm;
        break;
      }
    }

    int finalExitCode = 0;
    if (foundPackageManager == null) {
      context.warning("No supported Package Manager found for installation");
      return false;
    } else {
      List<String> commandList = commands.get(foundPackageManager);
      if (commandList != null) {
        for (String command : commandList) {
          ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(bashPath)
              .addArgs("-c", command);
          finalExitCode = pc.run();
        }
      }
    }

    if (finalExitCode == 0) {
      this.context.success("Successfully installed {}", this.tool);
    } else {
      this.context.warning("{} was not successfully installed", this.tool);
      return false;
    }
    postInstall();
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
    VersionIdentifier selectedVersion = securityRiskInteraction(configuredVersion);
    setVersion(selectedVersion, silent);
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, selectedVersion);
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
      downloadBinaryPath = fileAccess.findFirst(downloadBinaryPath, Files::isExecutable, false);
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
