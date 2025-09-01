package com.devonfw.tools.ide.tool.yarn;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Yarn(IdeContext context) {

    super(context, "yarn", Set.of(Tag.JAVA_SCRIPT, Tag.BUILD));
  }

  private boolean hasNodeBinary(String binary) {

    Path toolPath = getToolBinPath();
    Path yarnPath = toolPath.resolve(binary);
    return Files.exists(yarnPath);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    if (hasNodeBinary("yarn.js")) {
      String version = this.context.newProcess().runAndGetSingleOutput("yarn", "-v");
      return VersionIdentifier.of(version);
    }
    this.context.debug("Yarn is not installed in {}", getToolPath());
    return null;
  }

  @Override
  public String getInstalledEdition() {

    if (hasNodeBinary("yarn.js")) {
      return "yarn";
    }
    return null;
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext pc, String edition) {

    ToolInstallation installation = super.installTool(version, pc, edition);
    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    // UrlUpdater required for Yarn...
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);
    installToolDependencies(resolvedVersion, edition, pc);
    if (!resolvedVersion.equals(getInstalledVersion())) {
      runNpmUninstall("yarn");
      runNpmInstall("corepack");
    } else {
      return new ToolInstallation(installation.rootDir(), installation.linkDir(), installation.binDir(), configuredVersion, false);
    }

    String yarnPackage = "yarn";
    if (resolvedVersion.isPattern()) {
      this.context.warning("Yarn currently does not support version pattern: {}", resolvedVersion);
    } else {
      yarnPackage += "@" + resolvedVersion.toString();
    }
    pc.run("corepack", "prepare", yarnPackage, "--activate");
    pc.run("corepack", "install", "-g", yarnPackage);
    return new ToolInstallation(installation.rootDir(), installation.linkDir(), installation.binDir(), configuredVersion, true);
  }

  private void runNpmUninstall(String npmPackage) {
    if (hasNodeBinary("yarn.js")) {
      ProcessResult result = runNpm("uninstall", "-g", npmPackage);
      if (result.isSuccessful()) {
        this.context.info("Successfully uninstalled {}", getName());
      } else {
        this.context.error("An error occurred while uninstalling {}", getName(), result.getErr());
      }
    }
  }

  private ProcessResult runNpmInstall(String npmPackage) {

    return runNpm("install", "-g", npmPackage);
  }

  private ProcessResult runNpm(String... args) {

    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    return npm.runTool(args);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
