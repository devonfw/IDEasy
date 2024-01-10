package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyInfo;
import com.devonfw.tools.ide.url.model.file.dependencyJson.DependencyJson;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.devonfw.tools.ide.version.VersionRange;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * {@link ToolCommandlet} that is installed locally into the IDE.
 */
public abstract class LocalToolCommandlet extends ToolCommandlet {

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private static final String DEPENDENCY_FILENAME = "dependencies.json";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   *        method.
   */
  public LocalToolCommandlet(IdeContext context, String tool, Set<String> tags) {

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

    VersionIdentifier configuredVersion = getConfiguredVersion();
    // install configured version of our tool in the software repository if not already installed
    ToolInstallation installation = installInRepo(configuredVersion);

    if (Files.exists(getDependencyJsonPath())) {
      installDependency();
    } else {
      this.context.trace("No Dependencies file found");
    }

    // check if we already have this version installed (linked) locally in IDE_HOME/software
    VersionIdentifier installedVersion = getInstalledVersion();
    VersionIdentifier resolvedVersion = installation.resolvedVersion();
    if (isInstalledVersion(resolvedVersion, installedVersion, silent)) {
      return false;
    }
    // we need to link the version or update the link.
    Path toolPath = getToolPath();
    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.exists(toolPath)) {
      fileAccess.backup(toolPath);
    }
    fileAccess.symlink(installation.linkDir(), toolPath);
    this.context.getPath().setPath(this.tool, installation.binDir());
    if (installedVersion == null) {
      this.context.success("Successfully installed {} in version {}", this.tool, resolvedVersion);
    } else {
      this.context.success("Successfully installed {} in version {} replacing previous version {]", this.tool,
          resolvedVersion, installedVersion);
    }
    postInstall();
    return true;
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this {@link Commandlet} only in the central
   * software repository without touching the IDE installation.
   *
   * @param version the {@link VersionIdentifier} requested to be installed. May also be a
   *        {@link VersionIdentifier#isPattern() version pattern}.
   * @return the {@link ToolInstallation} in the central software repository matching the given {@code version}.
   */
  public ToolInstallation installInRepo(VersionIdentifier version) {

    return installInRepo(version, getEdition());
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this {@link Commandlet} only in the central
   * software repository without touching the IDE installation.
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
   * Performs the installation of the {@link #getName() tool} managed by this {@link Commandlet} only in the central
   * software repository without touching the IDE installation.
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
        this.context.debug("Version {} of tool {} is already installed at {}", resolvedVersion,
            getToolWithEdition(this.tool, edition), toolPath);
        return createToolInstallation(toolPath, resolvedVersion, toolVersionFile);
      }
      this.context.warning("Deleting corrupted installation at {}", toolPath);
      fileAccess.delete(toolPath);
    }
    Path target = toolRepository.download(this.tool, edition, resolvedVersion);
    fileAccess.mkdirs(toolPath.getParent());
    extract(target, toolPath);
    try {
      Files.writeString(toolVersionFile, resolvedVersion.toString(), StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write version file " + toolVersionFile, e);
    }
    return createToolInstallation(toolPath, resolvedVersion, toolVersionFile);
  }

  private ToolInstallation createToolInstallation(Path rootDir, VersionIdentifier resolvedVersion,
      Path toolVersionFile) {

    Path linkDir = getMacOsHelper().findLinkDir(rootDir);
    Path binDir = linkDir;
    Path binFolder = binDir.resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(binFolder)) {
      binDir = binFolder;
    }
    if (linkDir != rootDir) {
      assert (!linkDir.equals(rootDir));
      this.context.getFileAccess().copy(toolVersionFile, linkDir, FileCopyMode.COPY_FILE_OVERRIDE);
    }
    return new ToolInstallation(rootDir, linkDir, binDir, resolvedVersion);
  }

  private boolean isInstalledVersion(VersionIdentifier expectedVersion, VersionIdentifier installedVersion,
      boolean silent) {

    if (expectedVersion.equals(installedVersion)) {
      IdeLogLevel level = IdeLogLevel.INFO;
      if (silent) {
        level = IdeLogLevel.DEBUG;
      }
      this.context.level(level).log("Version {} of tool {} is already installed", installedVersion,
          getToolWithEdition());
      return true;
    }
    return false;
  }

  /**
   * Method to get the Path of the dependencies Json file
   * 
   * @return the {@link Path} of the dependencies file for the tool
   */
  private Path getDependencyJsonPath() {

    Path urlsPath = this.context.getUrlsPath();
    Path toolPath = urlsPath.resolve(getName()).resolve(getEdition());
    return toolPath.resolve(DEPENDENCY_FILENAME);
  }

  private void installDependency() {

    List<DependencyInfo> dependencies = readJson();

    for (DependencyInfo dependencyInfo : dependencies) {
      VersionRange dependencyVersionRangeFound = VersionRange.of(dependencyInfo.getVersionRange());
      String dependencyName = dependencyInfo.getDependency();
      ToolCommandlet dependencyTool = this.context.getCommandletManager().getToolCommandlet(dependencyName);
      VersionIdentifier dependencyVersionToInstall = findDependencyVersionToInstall(dependencyVersionRangeFound,
          dependencyName);
      if (dependencyVersionToInstall == null) {
        continue;
      }
      String defaultToolRepositoryId = this.context.getDefaultToolRepository().getId();
      Path dependencyRepository = this.context.getSoftwareRepositoryPath().resolve(defaultToolRepositoryId)
          .resolve(dependencyName).resolve(dependencyTool.getEdition());

      if (versionExistsInRepository(dependencyRepository, dependencyVersionRangeFound)) {
        this.context.info("Necessary version of the dependency {} is already installed in repository",
            dependencyName);
      } else {
        this.context.info("The version {} of the dependency {} is being installed", dependencyVersionToInstall,
            dependencyName);
        LocalToolCommandlet dependencyLocal = (LocalToolCommandlet) dependencyTool;
        dependencyLocal.installInRepo(dependencyVersionToInstall);
        this.context.info("The version {} of the dependency {} was successfully installed",
            dependencyVersionToInstall, dependencyName);
      }
    }
  }

  private List<DependencyInfo> readJson() {

    Path dependencyJsonPath = getDependencyJsonPath();

    try (BufferedReader reader = Files.newBufferedReader(dependencyJsonPath)) {
      DependencyJson dependencyJson = MAPPER.readValue(reader, DependencyJson.class);
      return findDependenciesFromJson(dependencyJson.getDependencies(), getConfiguredVersion());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Method to search the List of versions available in the ide and find the right version to install
   *
   * @param dependencyVersionRangeFound the {@link VersionRange} of the dependency that was found that needs to be
   *        installed
   * @param dependency the {@link String} of the dependency tool
   *
   * @return {@link VersionIdentifier} of the dependency that is to be installed
   */
  private VersionIdentifier findDependencyVersionToInstall(VersionRange dependencyVersionRangeFound,
      String dependency) {

    String dependencyEdition = this.context.getVariables().getToolEdition(dependency);

    List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(dependency, dependencyEdition);

    for (VersionIdentifier vi : versions) {
      if (dependencyVersionRangeFound.contains(vi)) {
        return vi;
      }
    }
    return null;
  }

  /**
   * Method to check if in the repository of the dependency there is a Version greater or equal to the version range to
   * be installed
   *
   * @param dependencyRepositoryPath the {@link Path} of the dependency repository
   * @param dependencyVersionRangeFound the {@link VersionRange} of the dependency version to be installed
   *
   * @return the {@code true} if such version exists in repository already, or {@code false} otherwise
   */
  private boolean versionExistsInRepository(Path dependencyRepositoryPath, VersionRange dependencyVersionRangeFound) {

    try (Stream<Path> versions = Files.list(dependencyRepositoryPath)) {
      Iterator<Path> versionsIterator = versions.iterator();
      while (versionsIterator.hasNext()) {
        VersionIdentifier versionFound = VersionIdentifier.of(versionsIterator.next().getFileName().toString());
        if (dependencyVersionRangeFound.contains(versionFound)) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to iterate through " + dependencyRepositoryPath, e);
    }
    return false;
  }

  private List<DependencyInfo> findDependenciesFromJson(Map<String, List<DependencyInfo>> dependencies,
      VersionIdentifier toolVersionToCheck) {

    for (Map.Entry<String, List<DependencyInfo>> map : dependencies.entrySet()) {

      String versionKey = map.getKey();
      VersionIdentifier foundToolVersion = VersionIdentifier.of(versionKey);

      // if a newer (greater) version is available, that is not already in the Json file
      if (toolVersionToCheck.getStart().compareVersion(foundToolVersion.getStart()).isGreater()) {
        return null;
      } else if (foundToolVersion.matches(toolVersionToCheck)) {
        return map.getValue();
      }
    }
    return null;
  }
}
