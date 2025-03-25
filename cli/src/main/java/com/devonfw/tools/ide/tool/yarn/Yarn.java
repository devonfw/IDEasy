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
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends LocalToolCommandlet {

  private static final VersionIdentifier YARN_MODERN_MIN_VERSION = VersionIdentifier.of("2.0");

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
    VersionComparisonResult comparisonResult = resolvedVersion.compareVersion(YARN_MODERN_MIN_VERSION);
    // install yarn classic (1.x) or yarn modern (> 2.0)?
    boolean yarnClassic = comparisonResult == VersionComparisonResult.LESS;
    if (yarnClassic) {
      String yarnPackage = "yarn";
      if (resolvedVersion.isPattern()) {
        this.context.warning("Yarn currently does not support version pattern: {}", resolvedVersion);
      } else {
        yarnPackage += "@" + resolvedVersion.toString();
      }
      npmInstall(yarnPackage, pc);
    } else {
      if (!hasNodeBinary("corepack")) {
        if (hasNodeBinary("yarn")) {
          // uninstall yarn classic to prevent error when installing core-pack
          pc.run("npm", "uninstall", "-g", "yarn");
        }
        npmInstall("corepack", pc);
      }
      pc.run("yarn", "init", "-2");
      if (resolvedVersion.isPattern()) {
        this.context.warning("Yarn currently does not support version pattern: {}", resolvedVersion);
      } else {
        pc.run("yarn", "set", "version", resolvedVersion.toString());
        pc.run("yarn", "install");
      }
    }
    Path rootDir = null;
    return new ToolInstallation(rootDir, rootDir, null, configuredVersion, true);
  }

  private void npmInstall(String npmPackage, ProcessContext pc) {

    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    npm.arguments.clearValue();
    npm.arguments.addValue("install");
    npm.arguments.addValue("-g");
    npm.arguments.addValue(npmPackage);
    npm.run();
  }

  @Override
  public boolean install(boolean silent, ProcessContext pc) {

    return super.install(silent, pc);
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
