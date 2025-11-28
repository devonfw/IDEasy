package com.devonfw.tools.ide.tool.node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.corepack.Corepack;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for tools based on <a href="https://www.npmjs.com/">npm</a>.
 */
public abstract class NodeBasedCommandlet extends LocalToolCommandlet {

  private final CachedValue<VersionIdentifier> installedVersion;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public NodeBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
    this.installedVersion = new CachedValue<>(this::computeInstalledVersion);
  }

  /**
   * @return the computed value of the {@link #getInstalledVersion() installed version}.
   */
  protected abstract VersionIdentifier computeInstalledVersion();

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    // node and node.js are messy - see https://github.com/devonfw/IDEasy/issues/352
    return true;
  }

  /**
   * @return the package of this tool from the NPM registry.
   */
  public String getPackageName() {

    return this.tool;
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwarePath().resolve("node");
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return this.installedVersion.get();
  }

  @Override
  public boolean isInstalled() {

    return hasNodeBinary(this.tool);
  }

  @Override
  public String getInstalledEdition() {

    if (getInstalledVersion() != null) {
      return this.tool;
    }
    return null;
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    runPackageInstall(getPackageName() + "@" + resolvedVersion);
    this.installedVersion.invalidate();
  }

  /**
   * Checks if the tool can be uninstalled e.g. if the uninstall command for the tool should be disabled or not.
   *
   * @return {@code true} if the tool can be uninstalled, {@code false} if not.
   */
  protected boolean canBeUninstalled() {
    return true;
  }

  @Override
  protected void performUninstall(Path toolPath) {
    if (canBeUninstalled()) {
      runPackageUninstall(getPackageName());
      this.installedVersion.invalidate();
    } else {
      this.context.info("IDEasy does not support uninstalling the tool {} since this will break your installation.\n"
          + "If you really want to uninstall it, please uninstall the entire node installation:\n"
          + "ide uninstall node", getPackageName());
    }
  }

  /**
   * Checks if a provided binary can be found within node.
   *
   * @param binary name of the binary.
   * @return {@code true} if a binary was found in the node installation, {@code false} if not.
   */
  protected boolean hasNodeBinary(String binary) {

    return Files.exists(getToolBinPath().resolve(binary));
  }

  /**
   * Runs uninstall using the package manager.
   *
   * @param npmPackage the npm package to uninstall.
   */
  protected void runPackageUninstall(String npmPackage) {

    runPackageManager("uninstall", "-g", npmPackage).failOnError();
  }

  /**
   * Runs install using the package manager.
   *
   * @param npmPackage the npm package to install.
   */
  protected void runPackageInstall(String npmPackage) {

    runPackageManager("install", "-gf", npmPackage).failOnError();
  }

  /**
   * @param args the arguments for the package manager.
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runPackageManager(String... args) {

    return runPackageManager(ProcessMode.DEFAULT, ProcessErrorHandling.THROW_CLI, args);
  }

  /**
   * @param args the arguments for {@link com.devonfw.tools.ide.tool.corepack.Corepack}.
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runCorepack(String... args) {

    return runCorepack(ProcessMode.DEFAULT, ProcessErrorHandling.THROW_CLI, args);
  }

  /**
   * @param processMode the {@link ProcessMode}.
   * @param errorHandling the {@link ProcessErrorHandling}.
   * @param args the arguments for the package manager.
   * @return the {@link ProcessResult}.
   */
  protected abstract ProcessResult runPackageManager(ProcessMode processMode, ProcessErrorHandling errorHandling, String... args);

  /**
   * @param processMode the {@link ProcessMode}.
   * @param errorHandling the {@link ProcessErrorHandling}.
   * @param args the arguments for {@link com.devonfw.tools.ide.tool.corepack.Corepack}.
   * @return the {@link ProcessResult}.
   */
  protected ProcessResult runCorepack(ProcessMode processMode, ProcessErrorHandling errorHandling, String... args) {
    ProcessContext pc = this.context.newProcess().errorHandling(errorHandling);
    Corepack corepack = this.context.getCommandletManager().getCommandlet(Corepack.class);
    return corepack.runTool(pc, processMode, args);
  }


  @Override
  public String getToolHelpArguments() {

    return "--help";
  }
}
