package com.devonfw.tools.ide.tool.ng;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
 * {@link ToolCommandlet} for <a href="https://angular.dev/">angular</a>.
 */
public class Ng extends LocalToolCommandlet {

  private final static String PACKAGE_ANGULAR_CLI = "@angular/cli";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Ng(IdeContext context) {

    super(context, "ng", Set.of(Tag.JAVA_SCRIPT, Tag.TYPE_SCRIPT, Tag.BUILD));
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
    Path ngPath = toolPath.resolve(binary);
    return Files.exists(ngPath);
  }

  private VersionIdentifier runNpmGetInstalledPackageVersion(String npmPackage) {
    if (hasNodeBinary(getName())) {
      List<String> versions;
      ProcessResult result = runNpm("list", "-g");
      if (result.isSuccessful()) {
        versions = result.getOut();
        String parsedVersion = "";
        for (String version : versions) {
          if (version.contains(npmPackage)) {
            parsedVersion = version.replaceAll(".*" + npmPackage + "@", "");
            break;
          }
        }
        return VersionIdentifier.of(parsedVersion);
      } else {
        this.context.debug("An error occurred while parsing the {} versions in {}", getName(), getToolPath(), result.getErr());
        return null;
      }
    } else {
      this.context.debug("{} is not installed in {}", getName(), getToolPath());
      return null;
    }
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return runNpmGetInstalledPackageVersion(PACKAGE_ANGULAR_CLI);
  }

  @Override
  public String getInstalledEdition() {

    if (hasNodeBinary(getName())) {
      return getName();
    }
    return null;
  }

  @Override
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext pc, String edition) {

    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);
    installToolDependencies(resolvedVersion, edition, pc);
    if (!resolvedVersion.equals(getInstalledVersion())) {
      runNpmUninstall(PACKAGE_ANGULAR_CLI);
    } else {
      return new ToolInstallation(null, null, null, configuredVersion, false);
    }
    String ngPackage = PACKAGE_ANGULAR_CLI;
    if (resolvedVersion.isPattern()) {
      this.context.warning("Ng currently does not support version pattern: {}", resolvedVersion);
    } else {
      ngPackage += "@" + resolvedVersion;
    }
    runNpmInstall(ngPackage);
    return new ToolInstallation(null, null, null, configuredVersion, true);
  }

  @Override
  public void uninstall() {
    runNpmUninstall(PACKAGE_ANGULAR_CLI);
  }

  private void runNpmUninstall(String npmPackage) {
    if (hasNodeBinary(getName())) {
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
