package com.devonfw.tools.ide.tool.yarn;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
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

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwarePath().resolve("node");
  }

  @Override
  public Path getToolBinPath() {

    return getToolPath();
  }

  private boolean hasNodeBinary(String binary) {

    Path toolPath = getToolPath();
    Path yarnPath = toolPath.resolve(binary);
    return Files.exists(yarnPath);
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    if (hasNodeBinary("yarn")) {
      String version = this.context.newProcess().runAndGetSingleOutput("yarn", "-v");
      return VersionIdentifier.of(version);
    }
    this.context.debug("Yarn is not installed in {}", getToolPath());
    return null;
  }

  @Override
  public String getInstalledEdition() {

    if (hasNodeBinary("yarn")) {
      return "yarn";
    }
    return null;
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext pc, String edition) {

    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    // UrlUpdater required for Yarn...
    // VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);
    VersionIdentifier resolvedVersion = configuredVersion;
    if (!hasNodeBinary("corepack")) {
      if (hasNodeBinary("yarn")) {
        // uninstall yarn classic to prevent error when installing core-pack
        pc.run("npm", "uninstall", "-g", "yarn");
      }
      runNpmInstall("corepack");
    }
    String yarnPackage = "yarn";
    if (resolvedVersion.isPattern()) {
      this.context.warning("Yarn currently does not support version pattern: {}", resolvedVersion);
    } else {
      yarnPackage += "@" + resolvedVersion.toString();
    }
    pc.run("corepack", "prepare", yarnPackage, "--activate");
    pc.run("corepack", "install", "-g", yarnPackage);
    Path rootDir = null;
    return new ToolInstallation(rootDir, rootDir, null, configuredVersion, true);
  }

  private void runNpmInstall(String npmPackage) {

    runNpm("install", "-g", npmPackage);
  }

  private void runNpm(String... args) {

    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    npm.runTool(args);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
