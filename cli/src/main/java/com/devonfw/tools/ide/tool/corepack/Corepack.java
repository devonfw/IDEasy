package com.devonfw.tools.ide.tool.corepack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.npm.Npm;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://www.npmjs.com/package/corepack">corepack</a>.
 */
public class Corepack extends LocalToolCommandlet {

  private static final String COREPACK_HOME_FOLDER = "corepack";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Corepack(IdeContext context) {

    super(context, "corepack", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  @Override
  public Path getToolBinPath() {

    return this.context.getSoftwarePath().resolve("corepack").resolve("shims");
  }

  private boolean hasNodeBinary(String binary) {

    Path toolPath = this.context.getSoftwarePath().resolve("node");
    Path binPath = toolPath.resolve(binary);
    if (!this.context.getSystemInfo().isWindows()) {
      binPath = toolPath.resolve("bin").resolve(binary);
    }
    return Files.exists(binPath);
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext pc, String edition) {

    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);

    if (resolvedVersion.equals(getInstalledVersion())) {
      // TODO: Fix repeatedly creating installations (always re-creates symlinks)
      ToolInstallation installation = super.installTool(version, pc, edition);
      return new ToolInstallation(installation.rootDir(), installation.linkDir(), installation.binDir(), configuredVersion, false);
    }

    // install node
    installToolDependencies(resolvedVersion, edition, pc);
    runNpmUninstall(getName());

    String corepackPackage = getName();

    if (resolvedVersion.isPattern()) {
      this.context.warning("Corepack currently does not support version pattern: {}", resolvedVersion);
    } else {
      corepackPackage += "@" + resolvedVersion;
    }

    runNpm("install", "-g", corepackPackage);
    pc.run(getName(), "enable");
    ToolInstallation installation = super.installTool(version, pc, edition);
    return new ToolInstallation(installation.rootDir(), installation.linkDir(), installation.binDir(), configuredVersion, true);
  }

  @Override
  public void uninstall() {
    runNpmUninstall(getName());
    super.uninstall();
  }

  private void runNpmUninstall(String npmPackage) {
    if (hasNodeBinary(getName())) {
      ProcessResult result = runNpm("uninstall", "-g", npmPackage);
      if (result.isSuccessful()) {
        this.context.info("Successfully uninstalled {}", npmPackage);
      } else {
        this.context.error("An error occurred while uninstalling {}", npmPackage, result.getErr());
      }
    }
  }

  private ProcessResult runNpm(String... args) {
    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    return npm.runTool(args);
  }

  /**
   * @return the {@link Path} to the corepack home folder, creates the folder if it was not existing.
   */
  public Path getOrCreateCorepackHomeFolder() {
    Path confPath = this.context.getConfPath();
    Path corepackConfigFolder = confPath.resolve(COREPACK_HOME_FOLDER);
    if (!Files.isDirectory(corepackConfigFolder)) {
      this.context.getFileAccess().mkdirs(corepackConfigFolder);
    }
    return corepackConfigFolder;
  }
}
