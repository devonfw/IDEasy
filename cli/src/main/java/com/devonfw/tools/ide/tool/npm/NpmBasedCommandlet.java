package com.devonfw.tools.ide.tool.npm;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for tools based on <a href="https://www.npmjs.com/">npm</a>.
 */
public abstract class NpmBasedCommandlet extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public NpmBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    // node and node.js are messy - see https://github.com/devonfw/IDEasy/issues/352
    return true;
  }

  /**
   * @return the package of this tool from the NPM registry.
   */
  protected abstract String getNpmPackage();

  @Override
  public Path getToolPath() {

    Path toolPath = this.context.getSoftwarePath().resolve("node");
    if (!this.context.getSystemInfo().isWindows()) {
      toolPath = toolPath.resolve("bin");
    }
    return toolPath;
  }

  protected VersionIdentifier runNpmGetInstalledPackageVersion(String npmPackage) {
    ProcessResult result = runNpm(ProcessMode.DEFAULT_CAPTURE, ProcessErrorHandling.NONE, "list", "-g", npmPackage, "--depth=0");
    if (result.isSuccessful()) {
      List<String> versions = result.getOut();
      assert versions.size() == 2;
      String parsedVersion = null;
      for (String version : versions) {
        if (version.contains(npmPackage)) {
          parsedVersion = version.replaceAll(".*" + npmPackage + "@", "");
          break;
        }
      }
      if (parsedVersion != null) {
        return VersionIdentifier.of(parsedVersion);
      }
    } else {
      this.context.debug("The npm package {} for tool {} is not installed.", npmPackage, this.tool);
    }
    return null;
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return runNpmGetInstalledPackageVersion(getNpmPackage());
  }

  @Override
  public String getInstalledEdition() {

    if (getInstalledVersion() != null) {
      return this.tool;
    }
    return null;
  }

  @Override
  protected void performToolInstallation(ToolRepository toolRepository, VersionIdentifier resolvedVersion, Path installationPath, String edition,
      ProcessContext processContext) {

    // runNpmUninstall(getNpmPackage()); // first uninstall a previously installed version
    runNpmInstall(getNpmPackage() + "@" + resolvedVersion);
  }

  @Override
  protected void performUninstall(Path toolPath) {

    runNpmUninstall(getNpmPackage());
  }

  /**
   * Performs a global npm uninstall.
   *
   * @param npmPackage the npm package to uninstall.
   */
  protected void runNpmUninstall(String npmPackage) {
    runNpm("uninstall", "-g", npmPackage).failOnError();
  }

  /**
   * Performs a global npm install.
   *
   * @param npmPackage the npm package to install.
   * @return the {@link ProcessResult} of the npm execution.
   */
  protected ProcessResult runNpmInstall(String npmPackage) {

    return runNpm("install", "-g", npmPackage);
  }

  private ProcessResult runNpm(String... args) {

    return runNpm(ProcessMode.DEFAULT, ProcessErrorHandling.THROW_CLI, args);
  }

  private ProcessResult runNpm(ProcessMode processMode, ProcessErrorHandling errorHandling, String... args) {

    ProcessContext pc = this.context.newProcess().errorHandling(errorHandling);
    Npm npm = this.context.getCommandletManager().getCommandlet(Npm.class);
    return npm.runTool(processMode, null, pc, args);
  }

}
