package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.common.Tags;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} for a tool integrated into the IDE.
 */
public abstract class ToolCommandlet extends Commandlet implements Tags {

  /** @see #getName() */
  protected final String tool;

  private final Set<Tag> tags;

  /** The commandline arguments to pass to the tool. */
  public final StringProperty arguments;

  private Path executionDirectory;

  private MacOsHelper macOsHelper;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public ToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context);
    this.tool = tool;
    this.tags = tags;
    addKeyword(tool);
    this.arguments = new StringProperty("", false, true, "args");
    initProperties();
  }

  /**
   * Add initial Properties to the tool
   */
  protected void initProperties() {

    add(this.arguments);
  }

  /**
   * @return the name of the tool (e.g. "java", "mvn", "npm", "node").
   */
  @Override
  public final String getName() {

    return this.tool;
  }

  /**
   * @return the name of the binary executable for this tool.
   */
  protected String getBinaryName() {

    return this.tool;
  }

  @Override
  public final Set<Tag> getTags() {

    return this.tags;
  }

  /**
   * @return the execution directory where the tool will be executed. Will be {@code null} by default leading to execution in the users current working
   *     directory where IDEasy was called.
   * @see #setExecutionDirectory(Path)
   */
  public Path getExecutionDirectory() {
    return this.executionDirectory;
  }

  /**
   * @param executionDirectory the new value of {@link #getExecutionDirectory()}.
   */
  public void setExecutionDirectory(Path executionDirectory) {
    this.executionDirectory = executionDirectory;
  }

  /**
   * @return the {@link EnvironmentVariables#getToolVersion(String) tool version}.
   */
  public VersionIdentifier getConfiguredVersion() {

    return this.context.getVariables().getToolVersion(getName());
  }

  /**
   * @return the {@link EnvironmentVariables#getToolEdition(String) tool edition}.
   */
  public String getConfiguredEdition() {

    return this.context.getVariables().getToolEdition(getName());
  }

  /**
   * @return the {@link #getName() tool} with its {@link #getConfiguredEdition() edition}. The edition will be omitted if same as tool.
   * @see #getToolWithEdition(String, String)
   */
  protected final String getToolWithEdition() {

    return getToolWithEdition(getName(), getConfiguredEdition());
  }

  /**
   * @param tool the tool name.
   * @param edition the edition.
   * @return the {@link #getName() tool} with its {@link #getConfiguredEdition() edition}. The edition will be omitted if same as tool.
   */
  protected static String getToolWithEdition(String tool, String edition) {

    if (tool.equals(edition)) {
      return tool;
    }
    return tool + "/" + edition;
  }

  @Override
  public void run() {

    runTool(this.arguments.asArray());
  }

  /**
   * @param args the command-line arguments to run the tool.
   * @see ToolCommandlet#runTool(ProcessMode, GenericVersionRange, String...)
   */
  public void runTool(String... args) {

    runTool(ProcessMode.DEFAULT, null, args);
  }

  /**
   * Ensures the tool is installed and then runs this tool with the given arguments.
   *
   * @param processMode the {@link ProcessMode}. Should typically be {@link ProcessMode#DEFAULT} or {@link ProcessMode#BACKGROUND}.
   * @param toolVersion the explicit {@link GenericVersionRange version} to run. Typically {@code null} to run the
   *     {@link #getConfiguredVersion() configured version}. Otherwise, the specified version will be used (from the software repository, if not compatible).
   * @param args the command-line arguments to run the tool.
   */
  public final void runTool(ProcessMode processMode, GenericVersionRange toolVersion, String... args) {

    runTool(processMode, toolVersion, ProcessErrorHandling.THROW_CLI, args);
  }

  /**
   * Ensures the tool is installed and then runs this tool with the given arguments.
   *
   * @param processMode the {@link ProcessMode}. Should typically be {@link ProcessMode#DEFAULT} or {@link ProcessMode#BACKGROUND}.
   * @param toolVersion the explicit {@link GenericVersionRange version} to run. Typically {@code null} to run the
   *     {@link #getConfiguredVersion() configured version}. Otherwise, the specified version will be used (from the software repository, if not compatible).
   * @param errorHandling the {@link ProcessErrorHandling}.
   * @param args the command-line arguments to run the tool.
   * @return the {@link ProcessResult result}.
   */
  public ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, ProcessErrorHandling errorHandling, String... args) {

    ProcessContext pc = this.context.newProcess().errorHandling(errorHandling);
    install(true, pc);
    return runTool(processMode, errorHandling, pc, args);
  }

  /**
   * @param processMode the {@link ProcessMode}. Should typically be {@link ProcessMode#DEFAULT} or {@link ProcessMode#BACKGROUND}.
   * @param errorHandling the {@link ProcessErrorHandling}.
   * @param pc the {@link ProcessContext}.
   * @param args the command-line arguments to run the tool.
   * @return the {@link ProcessResult result}.
   */
  public ProcessResult runTool(ProcessMode processMode, ProcessErrorHandling errorHandling, ProcessContext pc, String... args) {

    if (this.executionDirectory != null) {
      pc.directory(this.executionDirectory);
    }
    configureToolBinary(pc, processMode, errorHandling);
    configureToolArgs(pc, processMode, errorHandling, args);
    return pc.run(processMode);
  }

  /**
   * @param pc the {@link ProcessContext}.
   * @param processMode the {@link ProcessMode}.
   * @param errorHandling the {@link ProcessErrorHandling}.
   */
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode, ProcessErrorHandling errorHandling) {

    pc.executable(Path.of(getBinaryName()));
  }

  /**
   * @param pc the {@link ProcessContext}.
   * @param processMode the {@link ProcessMode}.
   * @param errorHandling the {@link ProcessErrorHandling}.
   * @param args the command-line arguments to {@link ProcessContext#addArgs(Object...) add}.
   */
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, ProcessErrorHandling errorHandling, String... args) {

    pc.addArgs(args);
  }

  /**
   * Creates a new {@link ProcessContext} from the given executable with the provided arguments attached.
   *
   * @param binaryPath path to the binary executable for this process
   * @param args the command-line arguments for this process
   * @return {@link ProcessContext}
   */
  protected ProcessContext createProcessContext(Path binaryPath, String... args) {

    return this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_ERR).executable(binaryPath).addArgs(args);
  }

  /**
   * Installs or updates the managed {@link #getName() tool}.
   *
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and nothing has changed.
   */
  public boolean install() {

    return install(true);
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this {@link com.devonfw.tools.ide.commandlet.Commandlet}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and nothing has changed.
   */
  public boolean install(boolean silent) {
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    return install(silent, pc);
  }

  /**
   * Installs or updates the managed {@link #getName() tool}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @param environmentContext the {@link EnvironmentContext} used to
   *     {@link LocalToolCommandlet#setEnvironment(EnvironmentContext, ToolInstallation, boolean) configure environment variables}.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and nothing has changed.
   */
  public abstract boolean install(boolean silent, EnvironmentContext environmentContext);

  /**
   * @return {@code true} to extract (unpack) the downloaded binary file, {@code false} otherwise.
   */
  protected boolean isExtract() {

    return true;
  }

  /**
   * @return the {@link MacOsHelper} instance.
   */
  protected MacOsHelper getMacOsHelper() {

    if (this.macOsHelper == null) {
      this.macOsHelper = new MacOsHelper(this.context);
    }
    return this.macOsHelper;
  }

  /**
   * @return the currently installed {@link VersionIdentifier version} of this tool or {@code null} if not installed.
   */
  public abstract VersionIdentifier getInstalledVersion();

  /**
   * @return the installed edition of this tool or {@code null} if not installed.
   */
  public abstract String getInstalledEdition();

  /**
   * Uninstalls the {@link #getName() tool}.
   */
  public abstract void uninstall();

  /**
   * @return the {@link ToolRepository}.
   */
  public ToolRepository getToolRepository() {

    return this.context.getDefaultToolRepository();
  }

  /**
   * List the available editions of this tool.
   */
  public void listEditions() {

    List<String> editions = getToolRepository().getSortedEditions(getName());
    for (String edition : editions) {
      this.context.info(edition);
    }
  }

  /**
   * List the available versions of this tool.
   */
  public void listVersions() {

    List<VersionIdentifier> versions = getToolRepository().getSortedVersions(getName(), getConfiguredEdition(), this);
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

    setVersion(version, hint, null);
  }

  /**
   * Sets the tool version in the environment variable configuration file.
   *
   * @param version the version to set. May also be a {@link VersionIdentifier#isPattern() version pattern}.
   * @param hint - {@code true} to print the installation hint, {@code false} otherwise.
   * @param destination - the destination for the property to be set
   */
  public void setVersion(VersionIdentifier version, boolean hint, EnvironmentVariablesFiles destination) {

    String edition = getConfiguredEdition();
    ToolRepository toolRepository = getToolRepository();
    VersionIdentifier versionIdentifier = toolRepository.resolveVersion(this.tool, edition, version, this);
    Objects.requireNonNull(versionIdentifier);

    EnvironmentVariables variables = this.context.getVariables();
    if (destination == null) {
      //use default location
      destination = EnvironmentVariablesFiles.SETTINGS;
    }
    EnvironmentVariables settingsVariables = variables.getByType(destination.toType());
    String name = EnvironmentVariables.getToolVersionVariable(this.tool);

    VersionIdentifier resolvedVersion = toolRepository.resolveVersion(this.tool, edition, version, this);
    if (version.isPattern()) {
      this.context.debug("Resolved version {} to {} for tool {}/{}", version, resolvedVersion, this.tool, edition);
    }
    settingsVariables.set(name, resolvedVersion.toString(), false);
    settingsVariables.save();
    this.context.info("{}={} has been set in {}", name, version, settingsVariables.getSource());
    EnvironmentVariables declaringVariables = variables.findVariable(name);
    if ((declaringVariables != null) && (declaringVariables != settingsVariables)) {
      this.context.warning("The variable {} is overridden in {}. Please remove the overridden declaration in order to make the change affect.", name,
          declaringVariables.getSource());
    }
    if (hint) {
      this.context.info("To install that version call the following command:");
      this.context.info("ide install {}", this.tool);
    }
  }

  /**
   * Sets the tool edition in the environment variable configuration file.
   *
   * @param edition the edition to set.
   */
  public void setEdition(String edition) {

    setEdition(edition, true);
  }

  /**
   * Sets the tool edition in the environment variable configuration file.
   *
   * @param edition the edition to set
   * @param hint - {@code true} to print the installation hint, {@code false} otherwise.
   */
  public void setEdition(String edition, boolean hint) {

    setEdition(edition, hint, null);
  }

  /**
   * Sets the tool edition in the environment variable configuration file.
   *
   * @param edition the edition to set
   * @param hint - {@code true} to print the installation hint, {@code false} otherwise.
   * @param destination - the destination for the property to be set
   */
  public void setEdition(String edition, boolean hint, EnvironmentVariablesFiles destination) {

    if ((edition == null) || edition.isBlank()) {
      throw new IllegalStateException("Edition has to be specified!");
    }

    if (destination == null) {
      //use default location
      destination = EnvironmentVariablesFiles.SETTINGS;
    }

    if (!getToolRepository().getSortedEditions(this.tool).contains(edition)) {
      this.context.warning("Edition {} seems to be invalid", edition);
    }
    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables settingsVariables = variables.getByType(destination.toType());
    String name = EnvironmentVariables.getToolEditionVariable(this.tool);
    settingsVariables.set(name, edition, false);
    settingsVariables.save();

    this.context.info("{}={} has been set in {}", name, edition, settingsVariables.getSource());
    EnvironmentVariables declaringVariables = variables.findVariable(name);
    if ((declaringVariables != null) && (declaringVariables != settingsVariables)) {
      this.context.warning("The variable {} is overridden in {}. Please remove the overridden declaration in order to make the change affect.", name,
          declaringVariables.getSource());
    }
    if (hint) {
      this.context.info("To install that edition call the following command:");
      this.context.info("ide install {}", this.tool);
    }
  }

  /**
   * Runs the tool's help command to provide the user with usage information.
   */
  @Override
  public void printHelp(NlsBundle bundle) {

    super.printHelp(bundle);
    String toolHelpArgs = getToolHelpArguments();
    if (toolHelpArgs != null && getInstalledVersion() != null) {
      ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.LOG_WARNING)
          .executable(Path.of(getBinaryName())).addArgs(toolHelpArgs);
      pc.run(ProcessMode.DEFAULT);
    }
  }

  /**
   * @return the tool's specific help command. Usually help, --help or -h. Return null if not applicable.
   */
  public String getToolHelpArguments() {

    return null;
  }

  /**
   * Creates a start script for the tool using the tool name.
   *
   * @param targetDir the {@link Path} of the installation where to create the script. If a "bin" sub-folder is present, the script will be created there
   *     instead.
   * @param binary name of the binary to execute from the start script.
   */
  protected void createStartScript(Path targetDir, String binary) {

    createStartScript(targetDir, binary, false);
  }

  /**
   * Creates a start script for the tool using the tool name.
   *
   * @param targetDir the {@link Path} of the installation where to create the script. If a "bin" sub-folder is present, the script will be created there
   *     instead.
   * @param binary name of the binary to execute from the start script.
   * @param background {@code true} to run the {@code binary} in background, {@code false} otherwise (foreground).
   */
  protected void createStartScript(Path targetDir, String binary, boolean background) {

    Path binFolder = targetDir.resolve("bin");
    if (!Files.exists(binFolder)) {
      if (this.context.getSystemInfo().isMac()) {
        MacOsHelper macOsHelper = getMacOsHelper();
        Path appDir = macOsHelper.findAppDir(targetDir);
        binFolder = macOsHelper.findLinkDir(appDir, binary);
      } else {
        binFolder = targetDir;
      }
      assert (Files.exists(binFolder));
    }
    Path bashFile = binFolder.resolve(getName());
    String bashFileContentStart = "#!/usr/bin/env bash\n\"$(dirname \"$0\")/";
    String bashFileContentEnd = "\" $@";
    if (background) {
      bashFileContentEnd += " &";
    }
    try {
      Files.writeString(bashFile, bashFileContentStart + binary + bashFileContentEnd);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assert (Files.exists(bashFile));
    context.getFileAccess().makeExecutable(bashFile);
  }

  @Override
  public void reset() {
    super.reset();
    this.executionDirectory = null;
  }
}
