package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.TarCompression;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.property.StringListProperty;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.util.FilenameUtil;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} for a tool integrated into the IDE.
 */
public abstract class ToolCommandlet extends Commandlet {

  /** @see #getName() */
  protected final String tool;

  private final StringListProperty arguments;

  private MacOsHelper macOsHelper;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   */
  public ToolCommandlet(IdeContext context, String tool) {

    super(context);
    this.tool = tool;
    addKeyword(tool);
    this.arguments = add(new StringListProperty("", false, "args"));
  }

  /**
   * @return the name of the tool (e.g. "java", "mvn", "npm", "node").
   */
  @Override
  public String getName() {

    return this.tool;
  }

  @Override
  public void run() {

    runTool(null, this.arguments.asArray());
  }

  /**
   * Ensures the tool is installed and then runs this tool with the given arguments.
   *
   * @param toolVersion the explicit version (pattern) to run. Typically {@code null} to ensure the configured version
   *        is installed and use that one. Otherwise the specified version will be installed in the software repository
   *        without touching and IDE installation and used to run.
   * @param args the commandline arguments to run the tool.
   */
  public void runTool(VersionIdentifier toolVersion, String... args) {

    install(true);
    if (toolVersion != null) {
      throw new UnsupportedOperationException("Not yet implemented!");
    }
    Path toolPath = getToolPath();
    FileAccess fileAccess = this.context.getFileAccess();
    Path binPath = fileAccess.findFirst(toolPath, path -> path.getFileName().toString().equals("bin"), false);
    if (binPath == null) {
      binPath = toolPath;
    }
    Path binary = fileAccess.findFirst(binPath, this::isBinary, false);
    if (binary == null) {
      throw new IllegalStateException("Could not find executable binary for " + getName() + " in " + binPath);
    }
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(binary)
        .addArgs(args);
    pc.run();
  }

  private boolean isBinary(Path path) {

    String filename = path.getFileName().toString();
    if (filename.equals(this.tool)) {
      return true;
    } else if (filename.startsWith(this.tool)) {
      String suffix = filename.substring(this.tool.length());
      return this.context.getSystemInfo().getOs().isExecutable(suffix);
    }
    return false;
  }

  /**
   * @return the {@link EnvironmentVariables#getToolEdition(String) tool edition}.
   */
  protected String getEdition() {

    return this.context.getVariables().getToolEdition(getName());
  }

  /**
   * @return the {@link #getName() tool} with its {@link #getEdition() edition}. The edition will be omitted if same as
   *         tool.
   * @see #getToolWithEdition(String, String)
   */
  protected final String getToolWithEdition() {

    return getToolWithEdition(getName(), getEdition());
  }

  /**
   * @param tool the tool name.
   * @param edition the edition.
   * @return the {@link #getName() tool} with its {@link #getEdition() edition}. The edition will be omitted if same as
   *         tool.
   */
  protected final static String getToolWithEdition(String tool, String edition) {

    if (tool.equals(edition)) {
      return tool;
    }
    return tool + "/" + edition;
  }

  /**
   * @return the {@link Path} where the tool is located (installed).
   */
  protected Path getToolPath() {

    return this.context.getSoftwarePath().resolve(getName());
  }

  /**
   * @return the {@link EnvironmentVariables#getToolVersion(String) tool version}.
   */
  public VersionIdentifier getConfiguredVersion() {

    return this.context.getVariables().getToolVersion(getName());
  }

  /**
   * @return the currently installed {@link VersionIdentifier version} of this tool or {@code null} if not installed.
   */
  public VersionIdentifier getInstalledVersion() {

    Path toolPath = getToolPath();
    if (!Files.isDirectory(toolPath)) {
      this.context.trace("Tool {} not installed in {}", getName(), toolPath);
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

  /**
   * Method to be called for {@link #install(boolean)} from dependent {@link Commandlet}s.
   */
  public void install() {

    install(true);
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this {@link Commandlet}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   */
  public void install(boolean silent) {

    doInstall(silent);
  }

  /**
   * Installs or updates the managed {@link #getName() tool}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   */
  protected void doInstall(boolean silent) {

    VersionIdentifier configuredVersion = getConfiguredVersion();
    // install configured version of our tool in the software repository if not already installed
    ToolInstallation installation = installInRepo(configuredVersion);

    // check if we have already have this version installed (linked) locally in IDE_HOME/software
    VersionIdentifier installedVersion = getInstalledVersion();
    VersionIdentifier resolvedVersion = installation.resolvedVersion();
    if (isInstalledVersion(resolvedVersion, installedVersion, silent)) {
      return;
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
    return;
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
      this.context.getFileAccess().copy(toolVersionFile, linkDir, true);
    }
    return new ToolInstallation(rootDir, linkDir, binDir, resolvedVersion);
  }

  private MacOsHelper getMacOsHelper() {

    if (this.macOsHelper == null) {
      this.macOsHelper = new MacOsHelper(this.context);
    }
    return this.macOsHelper;
  }

  /**
   * @return {@code true} to extract (unpack) the downloaded binary file, {@code false} otherwise.
   */
  protected boolean isExtract() {

    return true;
  }

  /**
   * @return the currently installed tool version or {@code null} if not found (tool not installed).
   */
  protected String getInstalledToolVersion() {

    Path toolPath = getToolPath();
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
      return Files.readString(toolVersionFile).trim();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file " + toolVersionFile, e);
    }
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
   * @param file the {@link Path} to the file to extract.
   * @param targetDir the {@link Path} to the directory where to extract (or copy) the file.
   */
  protected void extract(Path file, Path targetDir) {

    FileAccess fileAccess = this.context.getFileAccess();
    if (isExtract()) {
      this.context.trace("Trying to extract the downloaded file {} to {}.", file, targetDir);
      String extension = FilenameUtil.getExtension(file.getFileName().toString());
      this.context.trace("Determined file extension {}", extension);
      TarCompression tarCompression = TarCompression.of(extension);
      if (tarCompression != null) {
        fileAccess.untar(file, targetDir, tarCompression);
      } else if ("zip".equals(extension) || "jar".equals(extension)) {
        fileAccess.unzip(file, targetDir);
      } else if ("dmg".equals(extension)) {
        assert this.context.getSystemInfo().isMac();
        Path mountPath = this.context.getIdeHome().resolve(IdeContext.FOLDER_UPDATES).resolve(IdeContext.FOLDER_VOLUME);
        fileAccess.mkdirs(mountPath);
        ProcessContext pc = this.context.newProcess();
        pc.executable("hdiutil");
        pc.addArgs("attach", "-quiet", "-nobrowse", "-mountpoint", mountPath, file);
        pc.run();
        Path appPath = fileAccess.findFirst(mountPath, p -> p.getFileName().toString().endsWith(".app"), false);
        if (appPath == null) {
          throw new IllegalStateException("Failed to unpack DMG as no MacOS *.app was found in file " + file);
        }
        fileAccess.copy(appPath, targetDir);
        pc.addArgs("detach", "-force", mountPath);
        pc.run();
        // if [ -e "${target_dir}/Applications" ]
        // then
        // rm "${target_dir}/Applications"
        // fi
      } else if ("msi".equals(extension)) {
        this.context.newProcess().executable("msiexec").addArgs("//a", file, "//qn", "TARGETDIR=" + targetDir).run();
        // msiexec also creates a copy of the MSI
        Path msiCopy = targetDir.resolve(targetDir.getFileName());
        fileAccess.delete(msiCopy);
      } else if ("pkg".equals(extension)) {

        Path tmpDir = fileAccess.createTempDir("ide-pkg-");
        ProcessContext pc = this.context.newProcess();
        // we might also be able to use cpio from commons-compression instead of external xar...
        pc.executable("xar").addArgs("-C", tmpDir, "-xf", file).run();
        Path contentPath = fileAccess.findFirst(tmpDir, p -> p.getFileName().toString().equals("Payload"), true);
        fileAccess.untar(contentPath, targetDir, TarCompression.GZ);
        fileAccess.delete(tmpDir);
      } else {
        throw new IllegalStateException("Unknown archive format " + extension + ". Can not extract " + file);
      }
    } else {
      this.context.trace("Extraction is disabled for '{}' hence just moving the downloaded file {}.", getName(), file);
      fileAccess.move(file, targetDir);
    }
  }

  /**
   * List the available versions of this tool.
   */
  public void listVersions() {

    List<VersionIdentifier> versions = this.context.getUrls().getSortedVersions(getName(), getEdition());
    for (VersionIdentifier vi : versions) {
      this.context.info(vi.toString());
    }
  }

  /**
   * Sets the tool version in the environment variable configuration file.
   *
   * @param version the version (pattern) to set.
   */
  public void setVersion(String version) {

    if ((version == null) || version.isBlank()) {
      throw new IllegalStateException("Version has to be specified!");
    }
    VersionIdentifier configuredVersion = VersionIdentifier.of(version);
    if (!configuredVersion.isPattern() && !configuredVersion.isValid()) {
      this.context.warning("Version {} seems to be invalid", version);
    }
    setVersion(configuredVersion, true);
  }

  /**
   * Sets the tool version in the environment variable configuration file.
   *
   * @param version the version to set. May also be a {@link VersionIdentifier#isPattern() version pattern}.
   * @param hint - {@code true} to print the installation hint, {@code false} otherwise.
   */
  public void setVersion(VersionIdentifier version, boolean hint) {

    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables settingsVariables = variables.getByType(EnvironmentVariablesType.SETTINGS);
    String edition = getEdition();
    String name = EnvironmentVariables.getToolVersionVariable(this.tool);
    VersionIdentifier resolvedVersion = this.context.getUrls().getVersion(this.tool, edition, version);
    if (version.isPattern()) {
      this.context.debug("Resolved version {} to {} for tool {}/{}", version, resolvedVersion, this.tool, edition);
    }
    settingsVariables.set(name, resolvedVersion.toString(), false);
    settingsVariables.save();
    this.context.info("{}={} has been set in {}", name, version, settingsVariables.getSource());
    EnvironmentVariables declaringVariables = variables.findVariable(name);
    if ((declaringVariables != null) && (declaringVariables != settingsVariables)) {
      this.context.warning(
          "The variable {} is overridden in {}. Please remove the overridden declaration in order to make the change affect.",
          name, declaringVariables.getSource());
    }
    if (hint) {
      this.context.info("To install that version call the following command:");
      this.context.info("ide install {}", this.tool);
    }
  }

}
