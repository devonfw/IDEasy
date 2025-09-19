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

    Path toolPath = this.context.getSoftwarePath().resolve("node");
    Path binPath = toolPath.resolve(binary);
    if (!this.context.getSystemInfo().isWindows()) {
      binPath = toolPath.resolve("bin").resolve(binary);
    }
    return Files.exists(binPath);
  }

  @Override
  public String getInstalledEdition() {

    if (hasNodeBinary(getName())) {
      return "yarn";
    }
    return null;
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext pc, String edition) {

    ToolInstallation installation = super.installTool(version, pc, edition);
    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);

    if (resolvedVersion.equals(getInstalledVersion()) && hasNodeBinary(getName())) {
      // TODO: Fix repeatedly creating installations (always re-creates symlinks)
      return new ToolInstallation(installation.rootDir(), installation.linkDir(), installation.binDir(), configuredVersion, false);
    }

    // install corepack
    installToolDependencies(resolvedVersion, edition, pc);

    String yarnPackage = "yarn";

    if (resolvedVersion.isPattern()) {
      this.context.warning("Yarn currently does not support version pattern: {}", resolvedVersion);
    } else {
      yarnPackage += "@" + resolvedVersion;
    }

    pc.run("corepack", "prepare", yarnPackage, "--activate");
    pc.run("corepack", "install", "-g", yarnPackage);

    return new ToolInstallation(installation.rootDir(), installation.linkDir(), installation.binDir(), configuredVersion, true);
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

  @Override
  public void uninstall() {

    // TODO: Check if yarn could be uninstalled via corepack instead of uninstalling the whole corepack package
    runNpmUninstall("corepack");
    this.context.warning("Uninstalling corepack from node, be aware that this includes yarn and pnpm!");
    super.uninstall();
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
