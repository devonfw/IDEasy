package com.devonfw.tools.ide.tool;

import static com.devonfw.tools.ide.tool.SecurityRiskInteractionAnswer.LATEST;
import static com.devonfw.tools.ide.tool.SecurityRiskInteractionAnswer.LATEST_SAFE;
import static com.devonfw.tools.ide.tool.SecurityRiskInteractionAnswer.NEXT_SAFE;
import static com.devonfw.tools.ide.tool.SecurityRiskInteractionAnswer.STAY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.common.Tags;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.property.StringListProperty;
import com.devonfw.tools.ide.url.model.file.UrlSecurityJsonFile;
import com.devonfw.tools.ide.url.model.file.json.UrlSecurityWarning;
import com.devonfw.tools.ide.util.Pair;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} for a tool integrated into the IDE.
 */
public abstract class ToolCommandlet extends Commandlet implements Tags {

  /** @see #getName() */
  protected final String tool;

  private final Set<Tag> tags;

  /** The commandline arguments to pass to the tool. */
  public final StringListProperty arguments;

  private MacOsHelper macOsHelper;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   *        method.
   */
  public ToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context);
    this.tool = tool;
    this.tags = tags;
    addKeyword(tool);
    this.arguments = new StringListProperty("", false, "args");
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
  public String getName() {

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

  @Override
  public void run() {

    runTool(ProcessMode.DEFAULT, null, this.arguments.asArray());
  }

  /**
   * Ensures the tool is installed and then runs this tool with the given arguments.
   *
   * @param processMode see {@link ProcessMode}
   * @param toolVersion the explicit version (pattern) to run. Typically {@code null} to ensure the configured version
   *        is installed and use that one. Otherwise, the specified version will be installed in the software repository
   *        without touching and IDE installation and used to run.
   * @param args the command-line arguments to run the tool.
   */
  public void runTool(ProcessMode processMode, VersionIdentifier toolVersion, String... args) {

    Path binaryPath;
    Path toolPath = Path.of(getBinaryName());
    if (toolVersion == null) {
      install(true);
      binaryPath = toolPath;
    } else {
      throw new UnsupportedOperationException("Not yet implemented!");
    }
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.WARNING).executable(binaryPath)
        .addArgs(args);

    pc.run(processMode);
  }

  /**
   * @param toolVersion the explicit {@link VersionIdentifier} of the tool to run.
   * @param args the command-line arguments to run the tool.
   * @see ToolCommandlet#runTool(ProcessMode, VersionIdentifier, String...)
   */
  public void runTool(VersionIdentifier toolVersion, String... args) {

    runTool(ProcessMode.DEFAULT, toolVersion, args);
  }

  /**
   * @return the {@link EnvironmentVariables#getToolEdition(String) tool edition}.
   */
  public String getEdition() {

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
   * @return the {@link EnvironmentVariables#getToolVersion(String) tool version}.
   */
  public VersionIdentifier getConfiguredVersion() {

    return this.context.getVariables().getToolVersion(getName());
  }

  /**
   * Method to be called for {@link #install(boolean)} from dependent
   * {@link com.devonfw.tools.ide.commandlet.Commandlet}s.
   *
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and
   *         nothing has changed.
   */
  public boolean install() {

    return install(true);
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this
   * {@link com.devonfw.tools.ide.commandlet.Commandlet}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and
   *         nothing has changed.
   */
  public boolean install(boolean silent) {

    return doInstall(silent);
  }

  /**
   * Checks if the given {@link VersionIdentifier} has a matching security warning in the {@link UrlSecurityJsonFile}.
   *
   * @param configuredVersion the {@link VersionIdentifier} to be checked.
   * @return the {@link VersionIdentifier} to be used for installation. If the configured version is safe or there are
   *         no save versions the potentially unresolved configured version is simply returned. Otherwise, a resolved
   *         version is returned.
   */
  protected VersionIdentifier securityRiskInteraction(VersionIdentifier configuredVersion) {

    UrlSecurityJsonFile securityFile = this.context.getUrls().getEdition(this.tool, this.getEdition())
        .getSecurityJsonFile();

    VersionIdentifier current = this.context.getUrls().getVersion(this.tool, this.getEdition(), configuredVersion);

    if (!securityFile.contains(current, true, this.context, securityFile.getParent())) {
      return configuredVersion;
    }

    List<VersionIdentifier> allVersions = this.context.getUrls().getSortedVersions(this.tool, this.getEdition());
    VersionIdentifier latest = allVersions.get(0);

    VersionIdentifier nextSafe = getNextSafeVersion(current, securityFile, allVersions);
    VersionIdentifier latestSafe = getLatestSafe(allVersions, securityFile);
    String cves = securityFile.getMatchingSecurityWarnings(current).stream().map(UrlSecurityWarning::getCveName)
        .collect(Collectors.joining(", "));
    String currentIsUnsafe = "Currently, version " + current + " of " + this.getName() + " is selected, "
        + "which has one or more vulnerabilities:\n\n" + cves + "\n\n(See also " + securityFile.getPath() + ")\n\n";

    if (latestSafe == null) {
      this.context.warning(currentIsUnsafe + "There is no safe version available.");
      return configuredVersion;
    }

    return whichVersionDoYouWantToInstall(configuredVersion, current, nextSafe, latestSafe, latest, currentIsUnsafe);
  }

  /**
   * Using all the safety information about the versions, this method asks the user which version to install.
   *
   * @param configuredVersion the version that was configured in the environment variables.
   * @param current the current version that was resolved from the configured version.
   * @param nextSafe the next safe version.
   * @param latestSafe the latest safe version.
   * @param latest the latest version.
   * @param currentIsUnsafe the message that is printed if the current version is unsafe.
   * @return the version that the user wants to install.
   */
  private VersionIdentifier whichVersionDoYouWantToInstall(VersionIdentifier configuredVersion,
      VersionIdentifier current, VersionIdentifier nextSafe, VersionIdentifier latestSafe, VersionIdentifier latest,
      String currentIsUnsafe) {

    String ask = "Which version do you want to install?";

    // enum id, option message, version that is returned if this option is selected
    Map<SecurityRiskInteractionAnswer, Pair<String, VersionIdentifier>> options = new HashMap<>();
    options.put(STAY, Pair.of("Stay with the current unsafe version (" + current + ").", configuredVersion));
    options.put(LATEST_SAFE, Pair.of("Install the latest of all safe versions (" + latestSafe + ").", latestSafe));
    options.put(LATEST,
        Pair.of("Install the latest version (" + latest + "). This version is save.", VersionIdentifier.LATEST));
    options.put(NEXT_SAFE, Pair.of("Install the next safe version (" + nextSafe + ").", nextSafe));

    if (current.equals(latest)) {
      return getAnswer(options, currentIsUnsafe + "There are no updates available. " + ask, STAY, LATEST_SAFE);
    } else if (nextSafe == null) { // install an older version that is safe or stay with the current unsafe version
      return getAnswer(options, currentIsUnsafe + "All newer versions are also not safe. " + ask, STAY, LATEST_SAFE);
    } else if (nextSafe.equals(latest)) {
      return getAnswer(options, currentIsUnsafe + "Of the newer versions, only the latest is safe. " + ask, STAY,
          LATEST);
    } else if (nextSafe.equals(latestSafe)) {
      return getAnswer(options, currentIsUnsafe + "Of the newer versions, only version " + nextSafe
          + " is safe, which is however not the latest. " + ask, STAY, NEXT_SAFE);
    } else {
      if (latestSafe.equals(latest)) {
        return getAnswer(options, currentIsUnsafe + ask, STAY, NEXT_SAFE, LATEST);
      } else {
        return getAnswer(options, currentIsUnsafe + ask, STAY, NEXT_SAFE, LATEST_SAFE);
      }
    }
  }

  private VersionIdentifier getAnswer(Map<SecurityRiskInteractionAnswer, Pair<String, VersionIdentifier>> options,
      String question, SecurityRiskInteractionAnswer... possibleAnswers) {

    String[] availableOptions = Arrays.stream(possibleAnswers).map(options::get).map(Pair::getFirst)
        .toArray(String[]::new);
    String answer = this.context.question(question, availableOptions);
    for (SecurityRiskInteractionAnswer possibleAnswer : possibleAnswers) {
      if (options.get(possibleAnswer).getFirst().equals(answer)) {
        return options.get(possibleAnswer).getSecond();
      }
    }
    throw new IllegalStateException("Invalid answer " + answer);
  }

  /**
   * Gets the latest safe version from the list of all versions.
   *
   * @param allVersions all versions of the tool.
   * @param securityFile the {@link UrlSecurityJsonFile} to check if a version is safe or not.
   * @return the latest safe version or {@code null} if there is no safe version.
   */
  private VersionIdentifier getLatestSafe(List<VersionIdentifier> allVersions, UrlSecurityJsonFile securityFile) {

    VersionIdentifier latestSafe = null;
    for (int i = 0; i < allVersions.size(); i++) {
      if (!securityFile.contains(allVersions.get(i), true, this.context, securityFile.getParent())) {
        latestSafe = allVersions.get(i);
        break;
      }
    }
    return latestSafe;
  }

  /**
   * Gets the next safe version from the list of all versions starting from the current version.
   *
   * @param current the current version.
   * @param securityFile the {@link UrlSecurityJsonFile} to check if a version is safe or not.
   * @param allVersions all versions of the tool.
   * @return the next safe version or {@code null} if there is no safe version.
   */
  private VersionIdentifier getNextSafeVersion(VersionIdentifier current, UrlSecurityJsonFile securityFile,
      List<VersionIdentifier> allVersions) {

    int currentVersionIndex = allVersions.indexOf(current);
    VersionIdentifier nextSafe = null;
    for (int i = currentVersionIndex - 1; i >= 0; i--) {
      if (!securityFile.contains(allVersions.get(i), true, this.context, securityFile.getParent())) {
        nextSafe = allVersions.get(i);
        break;
      }
    }
    return nextSafe;
  }

  /**
   * Installs or updates the managed {@link #getName() tool}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return {@code true} if the tool was newly installed, {@code false} if the tool was already installed before and
   *         nothing has changed.
   */
  protected abstract boolean doInstall(boolean silent);

  /**
   * This method is called after the tool has been newly installed or updated to a new version. Override it to add
   * custom post installation logic.
   */
  protected void postInstall() {

    // nothing to do by default
  }

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

  /**
   * @return the installed edition of this tool or {@code null} if not installed.
   */
  public String getInstalledEdition() {

    return getInstalledEdition(this.context.getSoftwarePath().resolve(getName()));
  }

  /**
   * @param toolPath the installation {@link Path} where to find currently installed tool. The name of the parent
   *        directory of the real path corresponding to the passed {@link Path path} must be the name of the edition.
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
        edition = getEdition();
      }
      return edition;
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't determine the edition of " + getName()
          + " from the directory structure of its software path " + toolPath
          + ", assuming the name of the parent directory of the real path of the software path to be the edition "
          + "of the tool.", e);
    }

  }

  /**
   * List the available editions of this tool.
   */
  public void listEditions() {

    List<String> editions = this.context.getUrls().getSortedEditions(getName());
    for (String edition : editions) {
      this.context.info(edition);
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

    if ((edition == null) || edition.isBlank()) {
      throw new IllegalStateException("Edition has to be specified!");
    }

    if (!Files.exists(this.context.getUrls().getEdition(getName(), edition).getPath())) {
      this.context.warning("Edition {} seems to be invalid", edition);

    }
    EnvironmentVariables variables = this.context.getVariables();
    EnvironmentVariables settingsVariables = variables.getByType(EnvironmentVariablesType.SETTINGS);
    String name = EnvironmentVariables.getToolEditionVariable(this.tool);
    settingsVariables.set(name, edition, false);
    settingsVariables.save();

    this.context.info("{}={} has been set in {}", name, edition, settingsVariables.getSource());
    EnvironmentVariables declaringVariables = variables.findVariable(name);
    if ((declaringVariables != null) && (declaringVariables != settingsVariables)) {
      this.context.warning(
          "The variable {} is overridden in {}. Please remove the overridden declaration in order to make the change affect.",
          name, declaringVariables.getSource());
    }
    if (hint) {
      this.context.info("To install that edition call the following command:");
      this.context.info("ide install {}", this.tool);
    }
  }

}
