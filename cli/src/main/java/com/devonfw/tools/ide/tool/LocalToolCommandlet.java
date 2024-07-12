package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyInfo;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * {@link ToolCommandlet} that is installed locally into the IDE.
 */
public abstract class LocalToolCommandlet extends ToolCommandlet {

  protected HashMap<String, String> dependenciesEnvVariableNames = null;

  protected HashMap<String, String> dependenciesEnvVariablePaths = new HashMap<>();

  private final Dependency dependency = new Dependency(this.context, this.tool);

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags    the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
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

  @Override
  protected boolean doInstall(boolean silent) {

    VersionIdentifier configuredVersion = getConfiguredVersion();
    // get installed version before installInRepo actually may install the software
    VersionIdentifier installedVersion = getInstalledVersion();
    Step step = this.context.newStep(silent, "Install " + this.tool, configuredVersion);
    try {
      // install configured version of our tool in the software repository if not already installed
      ToolInstallation installation = installInRepo(configuredVersion);
      // check if we already have this version installed (linked) locally in IDE_HOME/software
      VersionIdentifier resolvedVersion = installation.resolvedVersion();
      if (resolvedVersion.equals(installedVersion) && !installation.newInstallation()) {
        IdeLogLevel level = silent ? IdeLogLevel.DEBUG : IdeLogLevel.INFO;
        this.context.level(level).log("Version {} of tool {} is already installed", installedVersion, getToolWithEdition());
        step.success();
        return false;
      }
      // we need to link the version or update the link.
      Path toolPath = getToolPath();
      FileAccess fileAccess = this.context.getFileAccess();
      if (Files.exists(toolPath)) {
        fileAccess.backup(toolPath);
      }
      fileAccess.mkdirs(toolPath.getParent());
      fileAccess.symlink(installation.linkDir(), toolPath);
      this.context.getPath().setPath(this.tool, installation.binDir());
      postInstall();
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
   * Performs the installation of the {@link #getName() tool} managed by this {@link com.devonfw.tools.ide.commandlet.Commandlet} only in the central software
   * repository without touching the IDE installation.
   *
   * @param version the {@link VersionIdentifier} requested to be installed. May also be a {@link VersionIdentifier#isPattern() version pattern}.
   * @return the {@link ToolInstallation} in the central software repository matching the given {@code version}.
   */
  public ToolInstallation installInRepo(VersionIdentifier version) {

    return installInRepo(version, getConfiguredEdition());
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet} only in the central software repository without touching the
   * IDE installation.
   *
   * @param version the {@link VersionIdentifier} requested to be installed. May also be a
   *     {@link VersionIdentifier#isPattern() version pattern}.
   * @param edition the specific edition to install.
   * @return the {@link ToolInstallation} in the central software repository matching the given {@code version}.
   */
  public ToolInstallation installInRepo(VersionIdentifier version, String edition) {

    return installInRepo(version, edition, this.context.getDefaultToolRepository());
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this {@link com.devonfw.tools.ide.commandlet.Commandlet} only in the central software
   * repository without touching the IDE installation.
   *
   * @param version        the {@link VersionIdentifier} requested to be installed. May also be a {@link VersionIdentifier#isPattern() version pattern}.
   * @param edition the specific edition to install.
   * @param toolRepository the {@link ToolRepository} to use.
   * @return the {@link ToolInstallation} in the central software repository matching the given {@code version}.
   */
  public ToolInstallation installInRepo(VersionIdentifier version, String edition, ToolRepository toolRepository) {

    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version);

    if (Files.exists(this.dependency.getDependencyJsonPath(getConfiguredEdition()))) {
      installDependencies(resolvedVersion);
    } else {
      this.context.trace("No Dependencies file found");
    }

    Path toolPath = this.context.getSoftwareRepositoryPath().resolve(toolRepository.getId()).resolve(this.tool).resolve(edition)
            .resolve(resolvedVersion.toString());
    Path toolVersionFile = toolPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.isDirectory(toolPath)) {
      if (Files.exists(toolVersionFile)) {
        if (this.context.isForceMode()) {
          fileAccess.delete(toolPath);
        } else {
          this.context.debug("Version {} of tool {} is already installed at {}", resolvedVersion, getToolWithEdition(this.tool, edition), toolPath);
          return createToolInstallation(toolPath, resolvedVersion, toolVersionFile);
        }
      } else {
        this.context.warning("Deleting corrupted installation at {}", toolPath);
        fileAccess.delete(toolPath);
      }
    }
    Path target = toolRepository.download(this.tool, edition, resolvedVersion);
    fileAccess.mkdirs(toolPath.getParent());
    boolean extract = isExtract();
    if (!extract) {
      this.context.trace("Extraction is disabled for '{}' hence just moving the downloaded file {}.", this.tool, target);
    }
    fileAccess.extract(target, toolPath, this::postExtract, extract);
    try {
      Files.writeString(toolVersionFile, resolvedVersion.toString(), StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write version file " + toolVersionFile, e);
    }
    // newInstallation results in above conditions to be true if isForceMode is true or if the tool version file was
    // missing
    return createToolInstallation(toolPath, resolvedVersion, toolVersionFile, true);
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
    try {
      String version = Files.readString(toolVersionFile).trim();
      return VersionIdentifier.of(version);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file " + toolVersionFile, e);
    }
  }

  @Override
  public String getInstalledEdition() {

    return getInstalledEdition(this.context.getSoftwarePath().resolve(getName()));
  }

  /**
   * @param toolPath the installation {@link Path} where to find currently installed tool. The name of the parent
   *     directory of the real path corresponding to the passed {@link Path path} must be the name of the edition.
   * @return the installed edition of this tool or {@code null} if not installed.
   */
  public String getInstalledEdition(Path toolPath) {

    if (!Files.isDirectory(toolPath)) {
      this.context.debug("Tool {} not installed in {}", getName(), toolPath);
      return null;
    }
    try {
      String edition = toolPath.toRealPath().getParent().getFileName().toString();
      if (!this.context.getUrls().getSortedEditions(getName()).contains(edition)) {
        edition = getConfiguredEdition();
      }
      return edition;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Couldn't determine the edition of " + getName() + " from the directory structure of its software path "
              + toolPath
              + ", assuming the name of the parent directory of the real path of the software path to be the edition "
              + "of the tool.", e);
    }

  }

  public void uninstall() {

    try {
      Path softwarePath = getToolPath();
      if (Files.exists(softwarePath)) {
        try {
          context.getFileAccess().delete(softwarePath);
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
                                                  boolean newInstallation) {

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
    return new ToolInstallation(rootDir, linkDir, binDir, resolvedVersion, newInstallation);
  }

  private ToolInstallation createToolInstallation(Path rootDir, VersionIdentifier resolvedVersion, Path toolVersionFile) {

    return createToolInstallation(rootDir, resolvedVersion, toolVersionFile, false);
  }

  @Override
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    Path binaryPath;
    Path toolPath = Path.of(getBinaryName());
    if (toolVersion == null) {
      install(true);
      binaryPath = toolPath;
    } else {
      throw new UnsupportedOperationException("Not yet implemented!");
    }

    if (Files.exists(this.dependency.getDependencyJsonPath(getConfiguredEdition()))) {
      setDependencyRepository(getInstalledVersion());
    } else {
      this.context.trace("No Dependencies file found");
    }

    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(binaryPath).addArgs(args);

    for (String key : this.dependenciesEnvVariablePaths.keySet()) {

      String dependencyPath = this.dependenciesEnvVariablePaths.get(key);
      pc = pc.withEnvVar(key, dependencyPath);
    }

    pc.run(processMode);
  }

  private void installDependencies(VersionIdentifier version) {

    List<DependencyInfo> dependencies = this.dependency.readJson(version, getConfiguredEdition());

    for (DependencyInfo dependencyInfo : dependencies) {

      String dependencyName = dependencyInfo.getTool();
      VersionIdentifier dependencyVersionToInstall = this.dependency.findDependencyVersionToInstall(dependencyInfo);
      if (dependencyVersionToInstall == null) {
        continue;
      }

      ToolCommandlet dependencyTool = this.context.getCommandletManager().getToolCommandlet(dependencyName);
      Path dependencyRepository = getDependencySoftwareRepository(dependencyName,
          dependencyTool.getConfiguredEdition());

      if (!Files.exists(dependencyRepository)) {
        installDependencyInRepo(dependencyName, dependencyTool, dependencyVersionToInstall);
      } else {
        Path versionExistingInRepository = this.dependency.versionExistsInRepository(dependencyRepository, dependencyInfo.getVersionRange());
        if (versionExistingInRepository.equals(Path.of(""))) {
          installDependencyInRepo(dependencyName, dependencyTool, dependencyVersionToInstall);
        } else {
          this.context.info("Necessary version of the dependency {} is already installed in repository", dependencyName);
        }
      }
    }
  }

  private void installDependencyInRepo(String dependencyName, ToolCommandlet dependencyTool, VersionIdentifier dependencyVersionToInstall) {

    this.context.info("The version {} of the dependency {} is being installed", dependencyVersionToInstall, dependencyName);
    LocalToolCommandlet dependencyLocal = (LocalToolCommandlet) dependencyTool;
    dependencyLocal.installInRepo(dependencyVersionToInstall);
    this.context.info("The version {} of the dependency {} was successfully installed", dependencyVersionToInstall, dependencyName);
  }

  protected void setDependencyRepository(VersionIdentifier version) {

    List<DependencyInfo> dependencies = this.dependency.readJson(version, getConfiguredEdition());

    for (DependencyInfo dependencyInfo : dependencies) {
      String dependencyName = dependencyInfo.getTool();
      VersionIdentifier dependencyVersionToInstall = this.dependency.findDependencyVersionToInstall(dependencyInfo);
      if (dependencyVersionToInstall == null) {
        continue;
      }

      ToolCommandlet dependencyTool = this.context.getCommandletManager().getToolCommandlet(dependencyName);
      Path dependencyRepository = getDependencySoftwareRepository(dependencyName,
          dependencyTool.getConfiguredEdition());
      Path versionExistingInRepository = this.dependency.versionExistsInRepository(dependencyRepository,
          dependencyInfo.getVersionRange());
      Path dependencyPath;

      if (versionExistingInRepository.equals(Path.of(""))) {
        dependencyPath = dependencyRepository.resolve(dependencyVersionToInstall.toString());
      } else {
        dependencyPath = dependencyRepository.resolve(versionExistingInRepository);
      }
      setDependencyEnvironmentPath(getDependencyEnvironmentName(dependencyName), dependencyPath);
    }
  }

  private void setDependencyEnvironmentPath(String dependencyEnvironmentName, Path dependencyPath) {

    this.dependenciesEnvVariablePaths.put(dependencyEnvironmentName, dependencyPath.toString());

  }

  /**
   * Method to return the list of the environment variable name for the dependencies. If necessary, it should be overridden in the specific tool
   *
   * @return the {@link HashMap} with the dependency name mapped to the env variable, for example ( java: JAVA_HOME )
   */

  protected HashMap<String, String> listOfDependencyEnvVariableNames() {

    return dependenciesEnvVariableNames;
  }

  private String getDependencyEnvironmentName(String dependencyName) {

    HashMap<String, String> envVariableName = listOfDependencyEnvVariableNames();

    if (envVariableName != null) {
      return envVariableName.get(dependencyName);
    }

    return dependencyName.toUpperCase() + "_HOME";
  }

  private Path getDependencySoftwareRepository(String dependencyName, String dependencyEdition) {

    String defaultToolRepositoryId = this.context.getDefaultToolRepository().getId();
    Path dependencyRepository = this.context.getSoftwareRepositoryPath().resolve(defaultToolRepositoryId).resolve(dependencyName).resolve(dependencyEdition);

    return dependencyRepository;
  }

}
