package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;

/**
 * {@link ToolCommandlet} that is installed locally into the IDEasy.
 */
public abstract class LocalToolCommandlet extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public LocalToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  /**
   * @return the {@link Path} where the tool is located (installed).
   */
  public Path getToolPath() {
    if (this.context.getSoftwarePath() == null) {
      return null;
    }
    return this.context.getSoftwarePath().resolve(getName());
  }

  /**
   * @return the {@link Path} where the executables of the tool can be found. Typically, a "bin" folder inside {@link #getToolPath() tool path}.
   */
  public Path getToolBinPath() {

    Path toolPath = getToolPath();
    if (toolPath == null) {
      return null;
    }
    Path binPath = toolPath.resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(binPath)) {
      return binPath;
    }
    return toolPath;
  }

  /**
   * @return {@code true} to ignore a missing {@link IdeContext#FILE_SOFTWARE_VERSION software version file} in an installation, {@code false} delete the broken
   *     installation (default).
   */
  protected boolean isIgnoreMissingSoftwareVersionFile() {

    return false;
  }

  /**
   * @deprecated will be removed once all "dependencies.json" are created in ide-urls.
   */
  @Deprecated
  protected void installDependencies() {

  }

  @Override
  protected ToolInstallation doInstall(ToolInstallRequest request) {

    installDependencies();
    Step step = request.getStep();
    if (step == null) {
      return doInstallStep(request);
    } else {
      return step.call(() -> doInstallStep(request),
          () -> createExistingToolInstallation(request));
    }
  }

  private ToolInstallation doInstallStep(ToolInstallRequest request) {

    // install configured version of our tool in the software repository if not already installed
    ToolInstallation installation = installTool(request);

    // check if we already have this version installed (linked) locally in IDE_HOME/software
    VersionIdentifier resolvedVersion = installation.resolvedVersion();
    if (request.isAlreadyInstalled()) {
      return installation;
    } else {
      this.context.debug("Installation from {} to {}.", request.getInstalled(), request.getRequested());
    }
    FileAccess fileAccess = this.context.getFileAccess();
    boolean ignoreSoftwareRepo = isIgnoreSoftwareRepo();
    if (!ignoreSoftwareRepo) {
      Path toolPath = getToolPath();
      // we need to link the version or update the link.
      if (Files.exists(toolPath, LinkOption.NOFOLLOW_LINKS)) {
        fileAccess.backup(toolPath);
      }
      fileAccess.mkdirs(toolPath.getParent());
      fileAccess.symlink(installation.linkDir(), toolPath);
    }
    if (installation.binDir() != null) {
      this.context.getPath().setPath(this.tool, installation.binDir());
    }
    postInstall(request);
    ToolEditionAndVersion installed = request.getInstalled();
    GenericVersionRange installedVersion = null;
    if (installed != null) {
      installedVersion = installed.getVersion();
    }
    ToolEditionAndVersion requested = request.getRequested();
    ToolEdition toolEdition = requested.getEdition();
    Step step = request.getStep();
    if (installedVersion == null) {
      asSuccess(step).log("Successfully installed {} in version {}", toolEdition, resolvedVersion);
    } else {
      asSuccess(step).log("Successfully installed {} in version {} replacing previous version {} of {}", toolEdition, resolvedVersion,
          installedVersion, installed.getEdition());
    }
    return installation;
  }

  /**
   * Determines whether this tool should be installed directly in the software folder or in the software repository.
   *
   * @return {@code true} if the tool should be installed directly in the software folder, ignoring the central software repository; {@code false} if the tool
   *     should be installed in the central software repository (default behavior).
   */
  protected boolean isIgnoreSoftwareRepo() {

    return false;
  }

  /**
   * Performs the installation of the {@link #getName() tool} together with the environment context  managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet}.
   *
   * @param request the {@link ToolInstallRequest}.
   * @return the resulting {@link ToolInstallation}.
   */
  public ToolInstallation installTool(ToolInstallRequest request) {

    completeRequest(request); // most likely already done, but if installTool was called directly and not from install
    if (request.isInstallLoop(this.context)) {
      return toolAlreadyInstalled(request);
    }
    ToolEditionAndVersion requested = request.getRequested();
    ToolEdition toolEdition = requested.getEdition();
    assert (toolEdition.tool().equals(this.tool)) : "Mismatch " + this.tool + " != " + toolEdition.tool();
    String edition = toolEdition.edition();
    VersionIdentifier resolvedVersion = cveCheck(request);
    installToolDependencies(request);

    // cveCheck might have changed resolvedVersion so let us re-check...
    if (request.isAlreadyInstalled()) {
      return toolAlreadyInstalled(request);
    } else {
      ToolEditionAndVersion installed = request.getInstalled();
      this.context.debug("Installation from {} to {}.", installed, requested);
    }
    Path installationPath = getInstallationPath(edition, resolvedVersion);

    ProcessContext processContext = request.getProcessContext();
    boolean additionalInstallation = request.isAdditionalInstallation();
    boolean ignoreSoftwareRepo = isIgnoreSoftwareRepo();
    Path toolVersionFile = installationPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.isDirectory(installationPath)) {
      if (Files.exists(toolVersionFile)) {
        if (!ignoreSoftwareRepo) {
          assert resolvedVersion.equals(getInstalledVersion(installationPath)) :
              "Found version " + getInstalledVersion(installationPath) + " in " + toolVersionFile + " but expected " + resolvedVersion;
          this.context.debug("Version {} of tool {} is already installed at {}", resolvedVersion, toolEdition, installationPath);
          return createToolInstallation(installationPath, resolvedVersion, false, processContext, additionalInstallation);
        }
      } else {
        // Makes sure that IDEasy will not delete itself
        if (this.tool.equals(IdeasyCommandlet.TOOL_NAME)) {
          this.context.warning("Your IDEasy installation is missing the version file at {}", toolVersionFile);
          return createToolInstallation(installationPath, resolvedVersion, false, processContext, additionalInstallation);
        } else if (!isIgnoreMissingSoftwareVersionFile()) {
          this.context.warning("Deleting corrupted installation at {}", installationPath);
          fileAccess.delete(installationPath);
        }
      }
    }
    VersionIdentifier actualInstalledVersion = performToolInstallation(request, installationPath, resolvedVersion);
    // If offline and could not download, actualInstalledVersion will be the old version, not resolvedVersion
    // In that case, we need to recalculate the installation path for the actually installed version
    if (!actualInstalledVersion.equals(resolvedVersion)) {
      installationPath = getInstallationPath(edition, actualInstalledVersion);
    }
    return createToolInstallation(installationPath, actualInstalledVersion, true, processContext, additionalInstallation);
  }

  /**
   * Performs the actual installation of the {@link #getName() tool} by downloading its binary, optionally extracting it, backing up any existing installation,
   * and writing the version file.
   * <p>
   * This method assumes that the version has already been resolved and dependencies installed. It handles the final steps of placing the tool into the
   * appropriate installation directory.
   *
   * @param request the {@link ToolInstallRequest}.
   * @param installationPath the target {@link Path} where the {@link #getName() tool} should be installed.
   * @param resolvedVersion the {@link VersionIdentifier} that should be installed.
   * @return the {@link VersionIdentifier} of the version that was actually installed. In offline scenarios where download fails, this may be different from
   *     {@code resolvedVersion} (returning the existing installed version instead).
   */
  protected VersionIdentifier performToolInstallation(ToolInstallRequest request, Path installationPath, VersionIdentifier resolvedVersion) {

    FileAccess fileAccess = this.context.getFileAccess();
    ToolEditionAndVersion requested = request.getRequested();
    Path downloadedToolFile;
    try {
      downloadedToolFile = downloadTool(requested.getEdition().edition(), resolvedVersion);
    } catch (CliOfflineException e) {
      // If we are offline and cannot download, check if we can continue with an existing installation
      ToolEditionAndVersion installed = request.getInstalled();
      if ((installed != null) && (installed.getResolvedVersion() != null)) {
        this.context.warning("Cannot download {} in version {} because we are offline. Continuing with already installed version {}.",
            this.tool, resolvedVersion, installed.getResolvedVersion());
        // Return the existing installed version to indicate fallback
        return installed.getResolvedVersion();
      }
      // No existing installation available, re-throw the exception
      throw e;
    }
    boolean extract = isExtract();
    if (!extract) {
      this.context.trace("Extraction is disabled for '{}' hence just moving the downloaded file {}.", this.tool, downloadedToolFile);
    }
    if (Files.isDirectory(installationPath)) {
      if (this.tool.equals(IdeasyCommandlet.TOOL_NAME)) {
        this.context.warning("Your IDEasy installation is missing the version file.");
      } else {
        fileAccess.backup(installationPath);
      }
    }
    fileAccess.mkdirs(installationPath.getParent());
    fileAccess.extract(downloadedToolFile, installationPath, this::postExtract, extract);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    this.context.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
    return resolvedVersion;
  }

  /**
   * @param edition the {@link #getConfiguredEdition() tool edition} to download.
   * @param resolvedVersion the resolved {@link VersionIdentifier version} to download.
   * @return the {@link Path} to the downloaded release file.
   */
  protected Path downloadTool(String edition, VersionIdentifier resolvedVersion) {
    return getToolRepository().download(this.tool, edition, resolvedVersion, this);
  }

  /**
   * Install this tool as dependency of another tool.
   *
   * @param versionRange the required {@link VersionRange}. See {@link ToolDependency#versionRange()}.
   * @param parentRequest the {@link ToolInstallRequest} of the tool causing this dependency.
   * @return the corresponding {@link ToolInstallation}.
   */
  public ToolInstallation installAsDependency(VersionRange versionRange, ToolInstallRequest parentRequest) {
    ToolInstallRequest request = new ToolInstallRequest(parentRequest);
    ToolEditionAndVersion requested = new ToolEditionAndVersion(getToolWithConfiguredEdition());
    request.setRequested(requested);
    VersionIdentifier configuredVersion = getConfiguredVersion();
    if (versionRange.contains(configuredVersion)) {
      // prefer configured version if contained in version range
      requested.setVersion(configuredVersion);
      // return install(true, configuredVersion, processContext, null);
      return install(request);
    } else {
      if (isIgnoreSoftwareRepo()) {
        throw new IllegalStateException(
            "Cannot satisfy dependency to " + this.tool + " in version " + versionRange + " for " + parentRequest.getRequested()
                + " since it is conflicting with configured version "
                + configuredVersion
                + " and this tool does not support the software repository.");
      }
      this.context.info(
          "The tool {} requires {} in the version range {}, but your project uses version {}, which does not match."
              + " Therefore, we install a compatible version in that range.",
          parentRequest.getRequested().getEdition(), this.tool, versionRange, configuredVersion);
      requested.setVersion(versionRange);
      return installTool(request);
    }
  }

  /**
   * Installs the tool dependencies for the current tool.
   *
   * @param request the {@link ToolInstallRequest}.
   */
  protected void installToolDependencies(ToolInstallRequest request) {

    ToolEditionAndVersion requested = request.getRequested();
    VersionIdentifier version = requested.getResolvedVersion();
    ToolEdition toolEdition = requested.getEdition();
    Collection<ToolDependency> dependencies = getToolRepository().findDependencies(this.tool, toolEdition.edition(), version);
    int size = dependencies.size();
    this.context.debug("Tool {} has {} other tool(s) as dependency", toolEdition, size);
    for (ToolDependency dependency : dependencies) {
      this.context.trace("Ensuring dependency {} for tool {}", dependency.tool(), toolEdition);
      LocalToolCommandlet dependencyTool = this.context.getCommandletManager().getRequiredLocalToolCommandlet(dependency.tool());
      dependencyTool.installAsDependency(dependency.versionRange(), request);
    }
  }

  /**
   * Post-extraction hook that can be overridden to add custom processing after unpacking and before moving to the final destination folder.
   *
   * @param extractedDir the {@link Path} to the folder with the unpacked tool.
   */
  protected void postExtract(Path extractedDir) {

  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return getInstalledVersion(getToolPath());
  }

  /**
   * @param toolPath the installation {@link Path} where to find the version file.
   * @return the currently installed {@link VersionIdentifier version} of this tool or {@code null} if not installed.
   */
  protected VersionIdentifier getInstalledVersion(Path toolPath) {

    if (isToolNotInstalled(toolPath)) {
      return null;
    }
    Path toolVersionFile = toolPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    if (!Files.exists(toolVersionFile)) {
      Path legacyToolVersionFile = toolPath.resolve(IdeContext.FILE_LEGACY_SOFTWARE_VERSION);
      if (Files.exists(legacyToolVersionFile)) {
        toolVersionFile = legacyToolVersionFile;
      } else {
        this.context.warning("Tool {} is missing version file in {}", getName(), toolVersionFile);
        return null;
      }
    }
    String version = this.context.getFileAccess().readFileContent(toolVersionFile).trim();
    return VersionIdentifier.of(version);
  }

  @Override
  public String getInstalledEdition() {

    return getInstalledEdition(getToolPath());
  }

  /**
   * @param toolPath the installation {@link Path} where to find currently installed tool. The name of the parent directory of the real path corresponding
   *     to the passed {@link Path path} must be the name of the edition.
   * @return the installed edition of this tool or {@code null} if not installed.
   */
  private String getInstalledEdition(Path toolPath) {
    if (isToolNotInstalled(toolPath)) {
      return null;
    }
    Path realPath = this.context.getFileAccess().toRealPath(toolPath);
    // if the realPath changed, a link has been resolved
    if (realPath.equals(toolPath)) {
      if (!isIgnoreSoftwareRepo()) {
        this.context.warning("Tool {} is not installed via software repository (maybe from devonfw-ide). Please consider reinstalling it.", this.tool);
      }
      // I do not see any reliable way how we could determine the edition of a tool that does not use software repo or that was installed by devonfw-ide
      return getConfiguredEdition();
    }
    Path toolRepoFolder = context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT).resolve(this.tool);
    String edition = getEdition(toolRepoFolder, realPath);
    if (edition == null) {
      edition = this.tool;
    }
    if (!getToolRepository().getSortedEditions(this.tool).contains(edition)) {
      this.context.warning("Undefined edition {} of tool {}", edition, this.tool);
    }
    return edition;
  }

  private String getEdition(Path toolRepoFolder, Path toolInstallFolder) {

    int toolRepoNameCount = toolRepoFolder.getNameCount();
    int toolInstallNameCount = toolInstallFolder.getNameCount();
    if (toolRepoNameCount < toolInstallNameCount) {
      // ensure toolInstallFolder starts with $IDE_ROOT/_ide/software/default/«tool»
      for (int i = 0; i < toolRepoNameCount; i++) {
        if (!toolRepoFolder.getName(i).toString().equals(toolInstallFolder.getName(i).toString())) {
          return null;
        }
      }
      return toolInstallFolder.getName(toolRepoNameCount).toString();
    }
    return null;
  }

  private Path getInstalledSoftwareRepoPath(Path toolPath) {
    if (isToolNotInstalled(toolPath)) {
      return null;
    }
    Path installPath = this.context.getFileAccess().toRealPath(toolPath);
    // if the installPath changed, a link has been resolved
    if (installPath.equals(toolPath)) {
      if (!isIgnoreSoftwareRepo()) {
        this.context.warning("Tool {} is not installed via software repository (maybe from devonfw-ide). Please consider reinstalling it.", this.tool);
      }
      // I do not see any reliable way how we could determine the edition of a tool that does not use software repo or that was installed by devonfw-ide
      return null;
    }
    installPath = getValidInstalledSoftwareRepoPath(installPath, context.getSoftwareRepositoryPath());
    return installPath;
  }

  Path getValidInstalledSoftwareRepoPath(Path installPath, Path softwareRepoPath) {
    int softwareRepoNameCount = softwareRepoPath.getNameCount();
    int toolInstallNameCount = installPath.getNameCount();
    int targetToolInstallNameCount = softwareRepoNameCount + 4;

    // installPath can't be shorter than softwareRepoPath
    if (toolInstallNameCount < softwareRepoNameCount) {
      this.context.warning("The installation path is not located within the software repository {}.", installPath);
      return null;
    }
    // ensure installPath starts with $IDE_ROOT/_ide/software/
    for (int i = 0; i < softwareRepoNameCount; i++) {
      if (!softwareRepoPath.getName(i).toString().equals(installPath.getName(i).toString())) {
        this.context.warning("The installation path is not located within the software repository {}.", installPath);
        return null;
      }
    }
    // return $IDE_ROOT/_ide/software/«id»/«tool»/«edition»/«version»
    if (toolInstallNameCount == targetToolInstallNameCount) {
      return installPath;
    } else if (toolInstallNameCount > targetToolInstallNameCount) {
      Path validInstallPath = installPath;
      for (int i = 0; i < toolInstallNameCount - targetToolInstallNameCount; i++) {
        validInstallPath = validInstallPath.getParent();
      }
      return validInstallPath;
    } else {
      this.context.warning("The installation path is faulty {}.", installPath);
      return null;
    }
  }

  private boolean isToolNotInstalled(Path toolPath) {

    if ((toolPath == null) || !Files.isDirectory(toolPath)) {
      this.context.debug("Tool {} not installed in {}", this.tool, toolPath);
      return true;
    }
    return false;
  }

  @Override
  public void uninstall() {
    try {
      Path toolPath = getToolPath();
      if (!Files.exists(toolPath)) {
        this.context.warning("An installed version of {} does not exist.", this.tool);
        return;
      }
      if (this.context.isForceMode() && !isIgnoreSoftwareRepo()) {
        this.context.warning(
            "You triggered an uninstall of {} in version {} with force mode!\n"
                + "This will physically delete the currently installed version from the machine.\n"
                + "This may cause issues with other projects, that use the same version of that tool."
            , this.tool, getInstalledVersion());
        uninstallFromSoftwareRepository(toolPath);
      }
      performUninstall(toolPath);
      this.context.success("Successfully uninstalled {}", this.tool);
    } catch (Exception e) {
      this.context.error(e, "Failed to uninstall {}", this.tool);
    }
  }

  /**
   * Performs the actual uninstallation of this tool.
   *
   * @param toolPath the current {@link #getToolPath() tool path}.
   */
  protected void performUninstall(Path toolPath) {
    this.context.getFileAccess().delete(toolPath);
  }

  /**
   * Deletes the installed version of the tool from the shared software repository.
   */
  private void uninstallFromSoftwareRepository(Path toolPath) {
    Path repoPath = getInstalledSoftwareRepoPath(toolPath);
    if ((repoPath == null) || !Files.exists(repoPath)) {
      this.context.warning("An installed version of {} does not exist in software repository.", this.tool);
      return;
    }
    this.context.info("Physically deleting {} as requested by the user via force mode.", repoPath);
    this.context.getFileAccess().delete(repoPath);
    this.context.success("Successfully deleted {} from your computer.", repoPath);
  }

  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {
    Path installationPath;
    if (isIgnoreSoftwareRepo()) {
      installationPath = getToolPath();
    } else {
      Path softwareRepositoryPath = this.context.getSoftwareRepositoryPath();
      if (softwareRepositoryPath == null) {
        return null;
      }
      Path softwareRepoPath = softwareRepositoryPath.resolve(getToolRepository().getId()).resolve(this.tool).resolve(edition);
      installationPath = softwareRepoPath.resolve(resolvedVersion.toString());
    }
    return installationPath;
  }

  /**
   * @return {@link VersionIdentifier} with latest version of the tool}.
   */
  public VersionIdentifier getLatestToolVersion() {

    return this.context.getDefaultToolRepository().resolveVersion(this.tool, getConfiguredEdition(), VersionIdentifier.LATEST, this);
  }


  /**
   * Searches for a wrapper file in valid projects (containing a build file f.e. build.gradle or pom.xml) and returns its path.
   *
   * @param wrapperFileName the name of the wrapper file
   * @return Path of the wrapper file or {@code null} if none was found.
   */
  protected Path findWrapper(String wrapperFileName) {
    Path dir = this.context.getCwd();
    // traverse the cwd directory containing a build descriptor up till a wrapper file was found
    while ((dir != null) && (findBuildDescriptor(dir) != null)) {
      Path wrapper = dir.resolve(wrapperFileName);
      if (Files.exists(wrapper)) {
        context.debug("Using wrapper: {}", wrapper);
        return wrapper;
      }
      dir = dir.getParent();
    }
    return null;
  }

  /**
   * @param directory the {@link Path} to the build directory.
   * @return the build configuration file for this tool or {@code null} if not found (or this is not a build tool).
   */
  public Path findBuildDescriptor(Path directory) {
    return null;
  }
}
