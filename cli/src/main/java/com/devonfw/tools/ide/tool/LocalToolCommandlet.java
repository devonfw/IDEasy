package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyInfo;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link ToolCommandlet} that is installed locally into the IDE.
 */
public abstract class LocalToolCommandlet extends ToolCommandlet {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final String DEPENDENCY_FILENAME = "dependencies.json";

  protected HashMap<String, String> dependenciesEnvVariableNames = new HashMap<>();

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   *        method.
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
   * @return the {@link Path} where the executables of the tool can be found. Typically a "bin" folder inside
   *         {@link #getToolPath() tool path}.
   */
  public Path getToolBinPath() {

    Path toolPath = getToolPath();
    Path binPath = this.context.getFileAccess().findFirst(toolPath, path -> path.getFileName().toString().equals("bin"),
        false);
    if ((binPath != null) && Files.isDirectory(binPath)) {
      return binPath;
    }
    return toolPath;
  }

  @Override
  protected boolean doInstall(boolean silent) {

    if (Files.exists(getDependencyJsonPath())) {
      installDependencies();
    } else {
      this.context.trace("No Dependencies file found");
    }

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
      step.end();
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

    return installInRepo(version, getEdition());
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet} only in the central software repository without touching the
   * IDE installation.
   *
   * @param version the {@link VersionIdentifier} requested to be installed. May also be a
   *        {@link VersionIdentifier#isPattern() version pattern}.
   * @param edition the specific edition to install.
   * @return the {@link ToolInstallation} in the central software repository matching the given {@code version}.
   */
  public ToolInstallation installInRepo(VersionIdentifier version, String edition) {

    return installInRepo(version, edition, this.context.getDefaultToolRepository());
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet} only in the central software repository without touching the
   * IDE installation.
   *
   * @param version the {@link VersionIdentifier} requested to be installed. May also be a
   *        {@link VersionIdentifier#isPattern() version pattern}.
   * @param edition the specific edition to install.
   * @param toolRepository the {@link ToolRepository} to use.
   * @return the {@link ToolInstallation} in the central software repository matching the given {@code version}.
   */
  public ToolInstallation installInRepo(VersionIdentifier version, String edition, ToolRepository toolRepository) {

    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version);
    Path toolPath = this.context.getSoftwareRepositoryPath().resolve(toolRepository.getId()).resolve(this.tool)
        .resolve(edition).resolve(resolvedVersion.toString());
    Path toolVersionFile = toolPath.resolve(IdeContext.FILE_SOFTWARE_VERSION);
    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.isDirectory(toolPath)) {
      if (Files.exists(toolVersionFile)) {
        if (this.context.isForceMode()) {
          fileAccess.delete(toolPath);
        } else {
          this.context.debug("Version {} of tool {} is already installed at {}", resolvedVersion,
              getToolWithEdition(this.tool, edition), toolPath);
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
      this.context.trace("Extraction is disabled for '{}' hence just moving the downloaded file {}.", this.tool,
          target);
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
   * Post-extraction hook that can be overridden to add custom processing after unpacking and before moving to the final
   * destination folder.
   *
   * @param extractedDir the {@link Path} to the folder with the unpacked tool.
   */
  protected void postExtract(Path extractedDir) {

  }

  private ToolInstallation createToolInstallation(Path rootDir, VersionIdentifier resolvedVersion, Path toolVersionFile,
      boolean newInstallation) {

    Path linkDir = getMacOsHelper().findLinkDir(rootDir, this.tool);
    Path binDir = linkDir;
    Path binFolder = binDir.resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(binFolder)) {
      binDir = binFolder;
    }
    if (linkDir != rootDir) {
      assert (!linkDir.equals(rootDir));
      this.context.getFileAccess().copy(toolVersionFile, linkDir.resolve(IdeContext.FILE_SOFTWARE_VERSION), FileCopyMode.COPY_FILE_OVERRIDE);
    }
    return new ToolInstallation(rootDir, linkDir, binDir, resolvedVersion, newInstallation);
  }

  private ToolInstallation createToolInstallation(Path rootDir, VersionIdentifier resolvedVersion, Path toolVersionFile) {

    return createToolInstallation(rootDir, resolvedVersion, toolVersionFile, false);
  }

  /**
   * Method to get the Path of the dependencies Json file
   *
   * @return the {@link Path} of the dependencies file for the tool
   */
  public Path getDependencyJsonPath() {

    Path toolPath = this.context.getUrlsPath().resolve(getName()).resolve(getEdition());
    return toolPath.resolve(DEPENDENCY_FILENAME);
  }

  private void installDependencies() {

    List<DependencyInfo> dependencies = readJson();

    for (DependencyInfo dependencyInfo : dependencies) {
      VersionRange dependencyVersionRangeFound = dependencyInfo.getVersionRange();
      String dependencyName = dependencyInfo.getTool();
      ToolCommandlet dependencyTool = this.context.getCommandletManager().getToolCommandlet(dependencyName);
      VersionIdentifier dependencyVersionToInstall = findDependencyVersionToInstall(dependencyInfo);
      if (dependencyVersionToInstall == null) {
        continue;
      }
      String defaultToolRepositoryId = this.context.getDefaultToolRepository().getId();
      Path dependencyRepository = this.context.getSoftwareRepositoryPath().resolve(defaultToolRepositoryId).resolve(dependencyName)
          .resolve(dependencyTool.getEdition());

      Path versionExistingInRepository = versionExistsInRepository(dependencyRepository, dependencyVersionRangeFound);

      if (versionExistingInRepository.equals(Path.of(""))) {
        this.context.info("The version {} of the dependency {} is being installed", dependencyVersionToInstall, dependencyName);
        LocalToolCommandlet dependencyLocal = (LocalToolCommandlet) dependencyTool;
        dependencyLocal.installInRepo(dependencyVersionToInstall);
        this.context.info("The version {} of the dependency {} was successfully installed", dependencyVersionToInstall, dependencyName);
        setDependencyEnvironmentPath(getDependencyEnvironmentName(dependencyName), dependencyRepository.resolve(dependencyVersionToInstall.toString()));
      } else {
        this.context.info("Necessary version of the dependency {} is already installed in repository", dependencyName);
        setDependencyEnvironmentPath(getDependencyEnvironmentName(dependencyName), versionExistingInRepository);
      }
    }
  }

  private void setDependencyEnvironmentPath(String dependencyEnvironmentName, Path dependencyPath) {

    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables typeVariables = variables.getByType(EnvironmentVariablesType.CONF);
    typeVariables.set(dependencyEnvironmentName, dependencyPath.toString(), true);
  }

  /**
   * Method to get return the specific name of the tool for setting the environment variable for the dependency. In the cases the environment variable of the
   * dependency is just the dependency name and _HOME, this method returns an empty string, and the variable is set automatically
   *
   * @return the {@link String} of the dependency environment variable name, for example JAVA_HOME
   */

  protected HashMap<String, String> listOfDependencyEnvVariableNames() {

    return dependenciesEnvVariableNames;
  }

  private String getDependencyEnvironmentName(String dependencyName) {

    String envVariableName = listOfDependencyEnvVariableNames().get(dependencyName);

    if (envVariableName != null) {
      return envVariableName;
    }

    return dependencyName + "_HOME";
  }

  private List<DependencyInfo> readJson() {

    Path dependencyJsonPath = getDependencyJsonPath();

    try (BufferedReader reader = Files.newBufferedReader(dependencyJsonPath)) {
      TypeReference<HashMap<VersionRange, List<DependencyInfo>>> typeRef = new TypeReference<>() {
      };
      Map<VersionRange, List<DependencyInfo>> dependencyJson = MAPPER.readValue(reader, typeRef);
      return findDependenciesFromJson(dependencyJson, getConfiguredVersion());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to search the List of versions available in the ide and find the right version to install
   *
   * @param dependencyFound the {@link DependencyInfo} of the dependency that was found that needs to be installed
   *
   * @return {@link VersionIdentifier} of the dependency that is to be installed
   */
  private VersionIdentifier findDependencyVersionToInstall(DependencyInfo dependencyFound) {

    String dependencyEdition = this.context.getVariables().getToolEdition(dependencyFound.getTool());

    List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(dependencyFound.getTool(),
        dependencyEdition);

    for (VersionIdentifier vi : versions) {
      if (dependencyFound.getVersionRange().contains(vi)) {
        return vi;
      }
    }
    return null;
  }

  /**
   * Method to check if in the repository of the dependency there is a Version greater or equal to the version range to be installed
   *
   * @param dependencyRepositoryPath the {@link Path} of the dependency repository
   * @param dependencyVersionRangeFound the {@link VersionRange} of the dependency version to be installed
   * @return the {@code true} if such version exists in repository already, or {@code false} otherwise
   */
  private Path versionExistsInRepository(Path dependencyRepositoryPath, VersionRange dependencyVersionRangeFound) {

    try (Stream<Path> versions = Files.list(dependencyRepositoryPath)) {
      Iterator<Path> versionsIterator = versions.iterator();
      while (versionsIterator.hasNext()) {
        VersionIdentifier versionFound = VersionIdentifier.of(versionsIterator.next().getFileName().toString());
        if (dependencyVersionRangeFound.contains(versionFound)) {
          assert versionFound != null;
          return dependencyRepositoryPath.resolve(versionFound.toString());
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to iterate through " + dependencyRepositoryPath, e);
    }
    return Path.of("");
  }

  private List<DependencyInfo> findDependenciesFromJson(Map<VersionRange, List<DependencyInfo>> dependencies, VersionIdentifier toolVersionToCheck) {

    for (Map.Entry<VersionRange, List<DependencyInfo>> map : dependencies.entrySet()) {

      VersionRange foundToolVersionRange = map.getKey();

      if (foundToolVersionRange.contains(toolVersionToCheck)) {
        return map.getValue();
      }
    }
    return null;
  }
}
