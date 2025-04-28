package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.process.EnvironmentContext;
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

    return this.context.getSoftwarePath().resolve(getName());
  }

  /**
   * @return the {@link Path} where the executables of the tool can be found. Typically a "bin" folder inside {@link #getToolPath() tool path}.
   */
  public Path getToolBinPath() {

    Path toolPath = getToolPath();
    Path binPath = this.context.getFileAccess().findFirst(toolPath, path -> path.getFileName().toString().equals("bin"), false);
    if ((binPath != null) && Files.isDirectory(binPath)) {
      return binPath;
    }
    return toolPath;
  }

  /**
   * @deprecated will be removed once all "dependencies.json" are created in ide-urls.
   */
  @Deprecated
  protected void installDependencies() {

  }

  @Override
  public boolean install(boolean silent, ProcessContext processContext) {

    installDependencies();
    VersionIdentifier configuredVersion = getConfiguredVersion();
    // get installed version before installInRepo actually may install the software
    VersionIdentifier installedVersion = getInstalledVersion();
    Step step = this.context.newStep(silent, "Install " + this.tool, configuredVersion);
    try {
      // install configured version of our tool in the software repository if not already installed
      ToolInstallation installation = installTool(configuredVersion, processContext);

      // check if we already have this version installed (linked) locally in IDE_HOME/software
      VersionIdentifier resolvedVersion = installation.resolvedVersion();
      if ((resolvedVersion.equals(installedVersion) && !installation.newInstallation())
          || (configuredVersion.matches(installedVersion) && context.isSkipUpdatesMode())) {
        return toolAlreadyInstalled(silent, installedVersion, step, processContext);
      }
      if (!isIgnoreSoftwareRepo()) {
        // we need to link the version or update the link.
        Path toolPath = getToolPath();
        FileAccess fileAccess = this.context.getFileAccess();
        if (Files.exists(toolPath, LinkOption.NOFOLLOW_LINKS)) {
          fileAccess.backup(toolPath);
        }
        fileAccess.mkdirs(toolPath.getParent());
        fileAccess.symlink(installation.linkDir(), toolPath);
      }
      this.context.getPath().setPath(this.tool, installation.binDir());
      postInstall(true, processContext);
      if (installedVersion == null) {
        step.success("Successfully installed {} in version {}", this.tool, resolvedVersion);
      } else {
        step.success("Successfully installed {} in version {} replacing previous version {}", this.tool, resolvedVersion, installedVersion);
      }
      return true;
    } catch (RuntimeException e) {
      step.error(e, true);
      throw e;
    } finally {
      step.close();
    }

  }

  /**
   * This method is called after a tool was requested to be installed or updated.
   *
   * @param newlyInstalled {@code true} if the tool was installed or updated (at least link to software folder was created/updated), {@code false} otherwise
   *     (configured version was already installed and nothing changed).
   * @param pc the {@link ProcessContext} to use.
   */
  protected void postInstall(boolean newlyInstalled, ProcessContext pc) {

    if (newlyInstalled) {
      postInstall();
    }
  }

  /**
   * This method is called after the tool has been newly installed or updated to a new version.
   */
  protected void postInstall() {

    // nothing to do by default
  }

  private boolean toolAlreadyInstalled(boolean silent, VersionIdentifier installedVersion, Step step, ProcessContext pc) {
    if (!silent) {
      this.context.info("Version {} of tool {} is already installed", installedVersion, getToolWithEdition());
    }
    postInstall(false, pc);
    step.success();
    return false;
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
   * Performs the installation of the {@link #getName() tool} together with the environment context, managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet}.
   *
   * @param version the {@link GenericVersionRange} requested to be installed.
   * @param processContext the {@link ProcessContext} used to
   *     {@link #setEnvironment(EnvironmentContext, ToolInstallation, boolean) configure environment variables}.
   * @return the {@link ToolInstallation} matching the given {@code version}.
   */
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext processContext) {

    return installTool(version, processContext, getConfiguredEdition());
  }

  /**
   * Performs the installation of the {@link #getName() tool} together with the environment context  managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet}.
   *
   * @param version the {@link GenericVersionRange} requested to be installed.
   * @param processContext the {@link ProcessContext} used to
   *     {@link #setEnvironment(EnvironmentContext, ToolInstallation, boolean) configure environment variables}.
   * @param edition the specific {@link #getConfiguredEdition() edition} to install.
   * @return the {@link ToolInstallation} matching the given {@code version}.
   */
  public ToolInstallation installTool(GenericVersionRange version, ProcessContext processContext, String edition) {

    // if version is a VersionRange, we are not called from install() but directly from installAsDependency() due to a version conflict of a dependency
    boolean extraInstallation = (version instanceof VersionRange);
    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);
    installToolDependencies(resolvedVersion, edition, processContext);

    Path installationPath;
    boolean ignoreSoftwareRepo = isIgnoreSoftwareRepo();
    if (ignoreSoftwareRepo) {
      installationPath = getToolPath();
    } else {
      Path softwareRepoPath = this.context.getSoftwareRepositoryPath().resolve(toolRepository.getId()).resolve(this.tool).resolve(edition);
      installationPath = softwareRepoPath.resolve(resolvedVersion.toString());
    }
    Path toolVersionFile = installationPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.isDirectory(installationPath)) {
      if (Files.exists(toolVersionFile)) {
        if (!ignoreSoftwareRepo || resolvedVersion.equals(getInstalledVersion())) {
          this.context.debug("Version {} of tool {} is already installed at {}", resolvedVersion, getToolWithEdition(this.tool, edition), installationPath);
          return createToolInstallation(installationPath, resolvedVersion, toolVersionFile, false, processContext, extraInstallation);
        }
      } else {
        // Makes sure that IDEasy will not delete itself
        if (this.tool.equals(IdeasyCommandlet.TOOL_NAME)) {
          this.context.warning("Your IDEasy installation is missing the version file at {}", toolVersionFile);
        } else {
          this.context.warning("Deleting corrupted installation at {}", installationPath);
          fileAccess.delete(installationPath);
        }
      }
    }
    Path downloadedToolFile = downloadTool(edition, toolRepository, resolvedVersion);
    boolean extract = isExtract();
    if (!extract) {
      this.context.trace("Extraction is disabled for '{}' hence just moving the downloaded file {}.", this.tool, downloadedToolFile);
    }
    if (Files.exists(installationPath)) {
      fileAccess.backup(installationPath);
    }
    fileAccess.mkdirs(installationPath.getParent());
    fileAccess.extract(downloadedToolFile, installationPath, this::postExtract, extract);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    this.context.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
    return createToolInstallation(installationPath, resolvedVersion, toolVersionFile, true, processContext, extraInstallation);
  }

  /**
   * @param edition the {@link #getConfiguredEdition() tool edition} to download.
   * @param toolRepository the {@link ToolRepository} to use.
   * @param resolvedVersion the resolved {@link VersionIdentifier version} to download.
   * @return the {@link Path} to the downloaded release file.
   */
  protected Path downloadTool(String edition, ToolRepository toolRepository, VersionIdentifier resolvedVersion) {
    return toolRepository.download(this.tool, edition, resolvedVersion, this);
  }

  /**
   * Install this tool as dependency of another tool.
   *
   * @param version the required {@link VersionRange}. See {@link ToolDependency#versionRange()}.
   * @param processContext the {@link ProcessContext}.
   * @return {@code true} if the tool was newly installed, {@code false} otherwise (installation was already present).
   */
  public boolean installAsDependency(VersionRange version, ProcessContext processContext) {

    VersionIdentifier configuredVersion = getConfiguredVersion();
    if (version.contains(configuredVersion)) {
      // prefer configured version if contained in version range
      return install(false, processContext);
    } else {
      if (isIgnoreSoftwareRepo()) {
        throw new IllegalStateException(
            "Cannot satisfy dependency to " + this.tool + " in version " + version + " since it is conflicting with configured version " + configuredVersion
                + " and this tool does not support the software repository.");
      }
      this.context.info(
          "Configured version of tool {} is {} but does not match version to install {} - need to use different version from software repository.",
          this.tool, configuredVersion, version);
    }
    ToolInstallation toolInstallation = installTool(version, processContext);
    return toolInstallation.newInstallation();
  }

  private void installToolDependencies(VersionIdentifier version, String edition, ProcessContext processContext) {
    Collection<ToolDependency> dependencies = getToolRepository().findDependencies(this.tool, edition, version);
    String toolWithEdition = getToolWithEdition(this.tool, edition);
    int size = dependencies.size();
    this.context.debug("Tool {} has {} other tool(s) as dependency", toolWithEdition, size);
    for (ToolDependency dependency : dependencies) {
      this.context.trace("Ensuring dependency {} for tool {}", dependency.tool(), toolWithEdition);
      LocalToolCommandlet dependencyTool = this.context.getCommandletManager().getRequiredLocalToolCommandlet(dependency.tool());
      dependencyTool.installAsDependency(dependency.versionRange(), processContext);
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

    return getInstalledVersion(this.context.getSoftwarePath().resolve(getName()));
  }

  /**
   * @param toolPath the installation {@link Path} where to find the version file.
   * @return the currently installed {@link VersionIdentifier version} of this tool or {@code null} if not installed.
   */
  protected VersionIdentifier getInstalledVersion(Path toolPath) {

    if (!Files.isDirectory(toolPath)) {
      this.context.debug("Tool {} not installed in {}", getName(), toolPath);
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

    if (this.context.getSoftwarePath() == null) {
      return "";
    }
    return getInstalledEdition(this.context.getSoftwarePath().resolve(this.tool));
  }

  /**
   * @param toolPath the installation {@link Path} where to find currently installed tool. The name of the parent directory of the real path corresponding
   *     to the passed {@link Path path} must be the name of the edition.
   * @return the installed edition of this tool or {@code null} if not installed.
   */
  private String getInstalledEdition(Path toolPath) {

    if (!Files.isDirectory(toolPath)) {
      this.context.debug("Tool {} not installed in {}", this.tool, toolPath);
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

  @Override
  public void uninstall() {

    try {
      Path softwarePath = getToolPath();
      if (Files.exists(softwarePath)) {
        try {
          this.context.getFileAccess().delete(softwarePath);
          this.context.success("Successfully uninstalled " + this.tool);
        } catch (Exception e) {
          this.context.error("Couldn't uninstall " + this.tool);
        }
      } else {
        this.context.warning("An installed version of " + this.tool + " does not exist");
      }
    } catch (Exception e) {
      this.context.error(e.getMessage());
    }
  }

  private ToolInstallation createToolInstallation(Path rootDir, VersionIdentifier resolvedVersion, Path toolVersionFile,
      boolean newInstallation, EnvironmentContext environmentContext, boolean extraInstallation) {

    Path linkDir = getMacOsHelper().findLinkDir(rootDir, getBinaryName());
    Path binDir = linkDir;
    Path binFolder = binDir.resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(binFolder)) {
      binDir = binFolder;
    }
    if (linkDir != rootDir) {
      assert (!linkDir.equals(rootDir));
      this.context.getFileAccess().copy(toolVersionFile, linkDir, FileCopyMode.COPY_FILE_OVERRIDE);
    }
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, linkDir, binDir, resolvedVersion, newInstallation);
    setEnvironment(environmentContext, toolInstallation, extraInstallation);
    return toolInstallation;
  }

  /**
   * Method to set environment variables for the process context.
   *
   * @param environmentContext the {@link EnvironmentContext} where to {@link EnvironmentContext#withEnvVar(String, String) set environment variables} for
   *     this tool.
   * @param toolInstallation the {@link ToolInstallation}.
   * @param extraInstallation {@code true} if the {@link ToolInstallation} is an additional installation to the
   *     {@link #getConfiguredVersion() configured version} due to a conflicting version of a {@link ToolDependency}, {@code false} otherwise.
   */
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    String pathVariable = EnvironmentVariables.getToolVariablePrefix(this.tool) + "_HOME";
    environmentContext.withEnvVar(pathVariable, toolInstallation.linkDir().toString());
    if (extraInstallation) {
      environmentContext.withPathEntry(toolInstallation.binDir());
    }
  }


}
