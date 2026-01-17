package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for tools that have their own {@link #getToolRepository() repository} and do not follow standard installation mechanism.
 *
 * @param <P> type of the {@link ToolCommandlet} acting as {@link #getPackageManagerClass() package manager}.
 */
public abstract class PackageManagerBasedLocalToolCommandlet<P extends ToolCommandlet> extends LocalToolCommandlet {

  private final CachedValue<VersionIdentifier> installedVersion;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public PackageManagerBasedLocalToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
    this.installedVersion = new CachedValue<>(this::determineInstalledVersion);
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    // python/pip and node/npm/yarn are messy - see https://github.com/devonfw/IDEasy/issues/352
    return true;
  }

  @Override
  public boolean isInstalled() {

    // Check if parent tool is installed first - if not, this tool cannot be installed
    LocalToolCommandlet parentTool = getParentTool();
    if (!parentTool.isInstalled()) {
      return false;
    }

    // Check if the tool binary folder exists
    Path toolBinPath = getToolBinPath();
    if (toolBinPath == null || !Files.isDirectory(toolBinPath)) {
      return false;
    }

    // Use SystemPath to properly resolve binary with platform-specific extensions (.exe, .cmd, .bat on Windows)
    Path binaryPath = toolBinPath.resolve(getBinaryName());
    Path resolvedBinary = this.context.getPath().findBinary(binaryPath);
    
    // findBinary returns the original path if not found, so check if it actually exists
    return Files.exists(resolvedBinary);
  }

  protected abstract Class<P> getPackageManagerClass();

  /**
   * @return the package name of this tool in the underlying repository (e.g. MVN repo, NPM registry, PyPI). Typically, this is the same as the
   *     {@link #getName() tool name} but may be overridden in some special cases.
   */
  public String getPackageName() {

    return this.tool;
  }

  /**
   * @param request the {@link PackageManagerRequest}.
   * @return the {@link ProcessResult}.
   */
  public ProcessResult runPackageManager(PackageManagerRequest request) {
    return runPackageManager(request, false);
  }

  /**
   * @param request the {@link PackageManagerRequest}.
   * @param skipInstallation {@code true} if the caller can guarantee that this package manager tool is already installed, {@code false} otherwise (run
   *     install method again to ensure the tool is installed).
   * @return the {@link ProcessResult}.
   */
  public ProcessResult runPackageManager(PackageManagerRequest request, boolean skipInstallation) {

    completeRequest(request);
    ProcessContext pc = request.getProcessContext();
    ToolCommandlet pm = request.getPackageManager();
    if (skipInstallation) { // See Node.postInstallOnNewInstallation
      return pm.runTool(pc, request.getProcessMode(), request.getArgs());
    } else {
      ToolInstallRequest installRequest = new ToolInstallRequest(true);
      installRequest.setProcessContext(pc);
      return pm.runTool(installRequest, request.getProcessMode(), request.getArgs());
    }
  }

  protected void completeRequest(PackageManagerRequest request) {

    if (request.getProcessContext() == null) {
      request.setProcessContext(this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI));
    }
    if (request.getPackageManager() == null) {
      request.setPackageManager(this.context.getCommandletManager().getCommandlet(getPackageManagerClass()));
    }
    if (request.getProcessMode() == null) {
      request.setProcessMode(ProcessMode.DEFAULT);
    }
    if (request.getArgs().isEmpty()) {
      completeRequestArgs(request);
    }
  }

  /**
   * @param request the {@link PackageManagerRequest} with currently {@link List#isEmpty() empty} {@link PackageManagerRequest#getArgs() args}.
   */
  protected void completeRequestArgs(PackageManagerRequest request) {

    String toolWithVersion = request.getTool();
    VersionIdentifier version = request.getVersion();
    if (version != null) {
      toolWithVersion = appendVersion(toolWithVersion, version);
    }
    request.addArg(request.getType());
    String option = completeRequestOption(request);
    if (option != null) {
      request.addArg(option);
    }
    request.addArg(toolWithVersion);
  }

  /**
   * @param request the {@link PackageManagerRequest}.
   * @return the option to {@link PackageManagerRequest#addArg(String) add as argument} to the package manager sub-command (e.g. "-gf") or {@code null} for no
   *     option.
   */
  protected String completeRequestOption(PackageManagerRequest request) {
    return null;
  }

  /**
   * @param tool the {@link PackageManagerRequest#getTool() tool} to manage (e.g. install).
   * @param version the {@link PackageManagerRequest#getVersion() version} to append.
   * @return the combination of {@code tool} and {@code version} in the syntax of the package manager.
   */
  protected String appendVersion(String tool, VersionIdentifier version) {
    return tool + '@' + version;
  }

  @Override
  protected boolean isIgnoreMissingSoftwareVersionFile() {

    return true;
  }

  private VersionIdentifier determineInstalledVersion() {

    try {
      return computeInstalledVersion();
    } catch (Exception e) {
      this.context.debug().log(e, "Failed to compute installed version of {}", this.tool);
      return null;
    }
  }

  /**
   * @return the computed value of the {@link #getInstalledVersion() installed version}.
   * @implNote Implementations of this method should NOT trigger any tool installation or download. If you need to call
   *     {@link #runPackageManager(PackageManagerRequest)}, make sure to use
   *     {@link #runPackageManager(PackageManagerRequest, boolean)} with {@code skipInstallation=true} to avoid
   *     inadvertently triggering installations when only checking the version.
   */
  protected abstract VersionIdentifier computeInstalledVersion();

  @Override
  public VersionIdentifier getInstalledVersion() {

    return this.installedVersion.get();
  }

  @Override
  protected final void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    PackageManagerRequest packageManagerRequest = new PackageManagerRequest(PackageManagerRequest.TYPE_INSTALL, getPackageName())
        .setProcessContext(request.getProcessContext()).setVersion(request.getRequested().getResolvedVersion());
    runPackageManager(packageManagerRequest, true).failOnError();
    this.installedVersion.invalidate();
  }

  /**
   * @return {@code true} if the tool can be uninstalled, {@code false} if not.
   */
  protected boolean canBeUninstalled() {
    return true;
  }

  @Override
  protected final void performUninstall(Path toolPath) {
    if (canBeUninstalled()) {
      PackageManagerRequest request = new PackageManagerRequest(PackageManagerRequest.TYPE_UNINSTALL, getPackageName());
      runPackageManager(request).failOnError();
      this.installedVersion.invalidate();
    } else {
      this.context.info("IDEasy does not support uninstalling the tool {} since this will break your installation.\n"
          + "If you really want to uninstall it, please uninstall its parent tool via:\n"
          + "ide uninstall {}", this.tool, getParentTool().getName());
    }
  }

  protected abstract LocalToolCommandlet getParentTool();

  @Override
  public Path getToolPath() {

    return getParentTool().getToolPath();
  }

}
