package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.json.mapping.JsonMapping;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * {@link ToolCommandlet} that is installed locally into the IDE.
 */
public abstract class LocalToolCommandlet extends ToolCommandlet {

  public List<String> dependencies = new ArrayList<>();

  private static final ObjectMapper MAPPER = JsonMapping.create();

  private final String dependencyString = "dependency";

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
      this.context.info("No Dependencies file found");
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

    final String dependencyFileName = "dependencies.json";
    Path URLspath = this.context.getUrlsPath();
    Path toolPath = URLspath.resolve(getName()).resolve(getEdition());
    return toolPath.resolve(dependencyFileName);
  }

  private void installDependency() {

    JsonNode nodeInJson = readJson();
    final String MinVersionString = "MinVersion";

    try {
      for (JsonNode node : nodeInJson) {
        VersionIdentifier dependencyVersionNumberFound = VersionIdentifier.of(node.get(MinVersionString).asText());
        String dependencyName = node.get(dependencyString).asText();
        ToolCommandlet dependencyTool = this.context.getCommandletManager().getToolCommandlet(dependencyName);
        VersionIdentifier dependencyVersionToInstall = findDependencyVersionToInstall(dependencyVersionNumberFound,
            dependencyName);

        String DefaultToolRepositoryID = this.context.getDefaultToolRepository().getId();
        Path dependencyRepository = this.context.getSoftwareRepositoryPath().resolve(DefaultToolRepositoryID)
            .resolve(dependencyName).resolve(dependencyTool.getEdition());

        if (versionExistsInRepository(dependencyRepository, dependencyVersionToInstall)) {
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
    } catch (Exception e) {
      this.context.error("Error occurred: {}", e);
    }
  }

  private JsonNode readJson() {

    Path dependencyJson = getDependencyJsonPath();

    try (BufferedReader reader = Files.newBufferedReader(dependencyJson)) {
      JsonNode node = MAPPER.readValue(reader, JsonNode.class);
      Map.Entry<String, JsonNode> entry = findDependenciesFromJson(node, getConfiguredVersion());
      if (entry == null) {
        this.context.warning(
            "The entry with the specified version was not found in the Json file. Please update dependencies Json file");
        return null;
      } else {
        return entry.getValue();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load " + dependencyJson, e);
    }
  }

  /**
   * Method to search the List of versions available in the ide and find the right version to install
   *
   * @param dependencyVersionNumberFound the {@link VersionIdentifier} of the dependency that was found that needs to be
   *        installed
   * @param dependency the {@link String} of the dependency tool
   *
   * @return {@link VersionIdentifier} of the dependency that is to be installed
   */
  private VersionIdentifier findDependencyVersionToInstall(VersionIdentifier dependencyVersionNumberFound,
      String dependency) {

    String dependencyEdition = this.context.getVariables().getToolEdition(dependency);

    List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(dependency, dependencyEdition);

    VersionIdentifier versionToReturn = null;
    for (VersionIdentifier vi : versions) {
      if (vi.compareVersion(dependencyVersionNumberFound).isGreater()) {
        versionToReturn = vi;
      }
    }
    return versionToReturn;
  }

  /**
   * Method to check if in the repository of the dependency there is a Version greater or equal to the version to be
   * installed
   *
   * @param dependencyRepositoryPath the {@link Path} of the dependency repository
   * @param dependencyVersionToInstall the {@link VersionIdentifier} of the dependency version to be installed
   *
   * @return the {@code true} if such version exists in repository already, or {@code false} otherwise
   */
  private boolean versionExistsInRepository(Path dependencyRepositoryPath,
      VersionIdentifier dependencyVersionToInstall) {

    try (Stream<Path> versions = Files.list(dependencyRepositoryPath)) {
      Iterator<Path> versionsIterator = versions.iterator();
      while (versionsIterator.hasNext()) {
        VersionIdentifier versionFound = VersionIdentifier.of(versionsIterator.next().getFileName().toString());
        if (!versionFound.compareVersion(dependencyVersionToInstall).isLess()) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to iterate through " + dependencyRepositoryPath, e);
    }
    return false;
  }

  /**
   * Method to find the dependency from the Json file
   *
   * @param toolVersions the {@link JsonNode} of the tool versions
   * @param toolVersionToCheck the {@link VersionIdentifier} of the tool to be searched in the Json file
   *
   * @return the {@link Map} of the searched version
   */
  private Map.Entry<String, JsonNode> findDependenciesFromJson(JsonNode toolVersions,
      VersionIdentifier toolVersionToCheck) {

    // Iterate through the fields of the Json file
    Iterator<Map.Entry<String, JsonNode>> fields = toolVersions.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      String versionKey = entry.getKey();
      VersionIdentifier foundToolVersion = VersionIdentifier.of(versionKey);

      // if a newer (greater) version is available, that is not already in the Json file
      if (toolVersionToCheck.getStart().compareVersion(foundToolVersion.getStart()).isGreater()) {
        return null;
      } else if (foundToolVersion.matches(toolVersionToCheck)) {
        this.dependencies.addAll(searchDependencyNames(entry.getValue()));
        return entry;
      }
    }
    return null;
  }

  /**
   * Method to find the names of the dependency names from the Json file
   *
   * @param dependenciesNode the {@link JsonNode} of the tool version
   *
   * @return the {@link List} of the dependencies as Strings
   */
  private List<String> searchDependencyNames(JsonNode dependenciesNode) {

    List<String> dependencyNames = new ArrayList<>();

    for (JsonNode dependencyNode : dependenciesNode) {
      String dependency = dependencyNode.get(dependencyString).asText();
      if (!dependencyNames.contains(dependency)) {
        dependencyNames.add(dependency);
      }
    }

    return dependencyNames;
  }

}
