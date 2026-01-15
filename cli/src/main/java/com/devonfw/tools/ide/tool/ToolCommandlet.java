package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.common.Tags;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesFiles;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.nls.NlsBundle;
import com.devonfw.tools.ide.os.MacOsHelper;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.property.StringProperty;
import com.devonfw.tools.ide.security.ToolVersionChoice;
import com.devonfw.tools.ide.security.ToolVulnerabilities;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.url.model.file.json.Cve;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.variable.IdeVariables;
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
   * @return the {@link ToolEdition} with {@link #getName() tool} with its {@link #getConfiguredEdition() edition}.
   */
  protected final ToolEdition getToolWithConfiguredEdition() {

    return new ToolEdition(this.tool, getConfiguredEdition());
  }

  @Override
  public void run() {

    runTool(this.arguments.asList());
  }

  /**
   * @param args the command-line arguments to run the tool.
   * @return the {@link ProcessResult result}.
   * @see ToolCommandlet#runTool(ProcessMode, GenericVersionRange, List)
   */
  public ProcessResult runTool(List<String> args) {

    return runTool(ProcessMode.DEFAULT, null, args);
  }

  /**
   * Ensures the tool is installed and then runs this tool with the given arguments.
   *
   * @param processMode the {@link ProcessMode}. Should typically be {@link ProcessMode#DEFAULT} or {@link ProcessMode#BACKGROUND}.
   * @param toolVersion the explicit {@link GenericVersionRange version} to run. Typically {@code null} to run the
   *     {@link #getConfiguredVersion() configured version}. Otherwise, the specified version will be used (from the software repository, if not compatible).
   * @param args the command-line arguments to run the tool.
   * @return the {@link ProcessResult result}.
   */
  public final ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, List<String> args) {

    return runTool(processMode, toolVersion, ProcessErrorHandling.THROW_CLI, args);
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
  public ProcessResult runTool(ProcessMode processMode, GenericVersionRange toolVersion, ProcessErrorHandling errorHandling, List<String> args) {

    ProcessContext pc = this.context.newProcess().errorHandling(errorHandling);
    ToolInstallRequest request = new ToolInstallRequest(true);
    if (toolVersion != null) {
      request.setRequested(new ToolEditionAndVersion(toolVersion));
    }
    request.setProcessContext(pc);
    return runTool(request, processMode, args);
  }

  /**
   * Ensures the tool is installed and then runs this tool with the given arguments.
   *
   * @param request the {@link ToolInstallRequest}.
   * @param processMode the {@link ProcessMode}. Should typically be {@link ProcessMode#DEFAULT} or {@link ProcessMode#BACKGROUND}.
   * @param args the command-line arguments to run the tool.
   * @return the {@link ProcessResult result}.
   */
  public ProcessResult runTool(ToolInstallRequest request, ProcessMode processMode, List<String> args) {

    if (request.isCveCheckDone()) {
      // if the CVE check has already been done, we can assume that the install(request) has already been called before
      // most likely a postInstall* method was overridden calling this method with the same request what is a programming error
      // we render this warning so the error gets detected and can be fixed but we do not block the user by skipping the installation.
      this.context.warning().log(new RuntimeException(), "Preventing infinity loop during installation of {}", request.getRequested());
    } else {
      install(request);
    }
    return runTool(request.getProcessContext(), processMode, args);
  }

  /**
   * @param pc the {@link ProcessContext}.
   * @param processMode the {@link ProcessMode}. Should typically be {@link ProcessMode#DEFAULT} or {@link ProcessMode#BACKGROUND}.
   * @param args the command-line arguments to run the tool.
   * @return the {@link ProcessResult result}.
   */
  public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {

    if (this.executionDirectory != null) {
      pc.directory(this.executionDirectory);
    }
    configureToolBinary(pc, processMode);
    configureToolArgs(pc, processMode, args);
    return pc.run(processMode);
  }

  /**
   * @param pc the {@link ProcessContext}.
   * @param processMode the {@link ProcessMode}.
   */
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode) {

    pc.executable(Path.of(getBinaryName()));
  }

  /**
   * @param pc the {@link ProcessContext}.
   * @param processMode the {@link ProcessMode}.
   * @param args the command-line arguments to {@link ProcessContext#addArgs(List) add}.
   */
  protected void configureToolArgs(ProcessContext pc, ProcessMode processMode, List<String> args) {

    pc.addArgs(args);
  }

  /**
   * Installs or updates the managed {@link #getName() tool}.
   *
   * @return the {@link ToolInstallation}.
   */
  public ToolInstallation install() {

    return install(true);
  }

  /**
   * Performs the installation of the {@link #getName() tool} managed by this {@link com.devonfw.tools.ide.commandlet.Commandlet}.
   *
   * @param silent - {@code true} if called recursively to suppress verbose logging, {@code false} otherwise.
   * @return the {@link ToolInstallation}.
   */
  public ToolInstallation install(boolean silent) {
    return install(new ToolInstallRequest(silent));
  }

  /**
   * Performs the installation (install, update, downgrade) of the {@link #getName() tool} managed by this {@link ToolCommandlet}.
   *
   * @param request the {@link ToolInstallRequest}.
   * @return the {@link ToolInstallation}.
   */
  public ToolInstallation install(ToolInstallRequest request) {

    completeRequest(request);
    if (request.isInstallLoop(this.context)) {
      return toolAlreadyInstalled(request);
    }
    return doInstall(request);
  }

  /**
   * Performs the installation (install, update, downgrade) of the {@link #getName() tool} managed by this {@link ToolCommandlet}.
   *
   * @param request the {@link ToolInstallRequest}.
   * @return the {@link ToolInstallation}.
   */
  protected abstract ToolInstallation doInstall(ToolInstallRequest request);

  /**
   * @param request the {@link ToolInstallRequest} to complete (fill values that are currently {@code null}).
   */
  protected void completeRequest(ToolInstallRequest request) {

    completeRequestInstalled(request);
    completeRequestRequested(request); // depends on completeRequestInstalled
    completeRequestProcessContext(request);
    completeRequestToolPath(request);
  }

  private void completeRequestProcessContext(ToolInstallRequest request) {
    if (request.getProcessContext() == null) {
      ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
      request.setProcessContext(pc);
    }
  }

  private void completeRequestInstalled(ToolInstallRequest request) {

    ToolEditionAndVersion installedToolVersion = request.getInstalled();
    if (installedToolVersion == null) {
      installedToolVersion = new ToolEditionAndVersion((GenericVersionRange) null);
      request.setInstalled(installedToolVersion);
    }
    Path toolPath = request.getToolPath();
    if (installedToolVersion.getVersion() == null) {
      VersionIdentifier installedVersion;
      if ((toolPath != null) && (this instanceof LocalToolCommandlet ltc)) {
        installedVersion = ltc.getInstalledVersion(toolPath);
      } else {
        installedVersion = getInstalledVersion();
      }
      if (installedVersion == null) {
        return;
      }
      installedToolVersion.setVersion(installedVersion);
    }
    if (installedToolVersion.getEdition() == null) {
      String installedEdition;
      if ((toolPath != null) && (this instanceof LocalToolCommandlet ltc)) {
        installedEdition = ltc.getInstalledEdition(toolPath);
      } else {
        installedEdition = getInstalledEdition();
      }
      installedToolVersion.setEdition(new ToolEdition(this.tool, installedEdition));
    }
    assert installedToolVersion.getResolvedVersion() != null;
  }

  private void completeRequestRequested(ToolInstallRequest request) {

    ToolEdition edition;
    ToolEditionAndVersion requested = request.getRequested();
    if (requested == null) {
      edition = new ToolEdition(this.tool, getConfiguredEdition());
      requested = new ToolEditionAndVersion(edition);
      request.setRequested(requested);
    } else {
      edition = requested.getEdition();
      if (edition == null) {
        edition = new ToolEdition(this.tool, getConfiguredEdition());
        requested.setEdition(edition);
      }
    }
    GenericVersionRange version = requested.getVersion();
    if (version == null) {
      version = getConfiguredVersion();
      requested.setVersion(version);
    }
    VersionIdentifier resolvedVersion = requested.getResolvedVersion();
    if (resolvedVersion == null) {
      if (this.context.isSkipUpdatesMode()) {
        ToolEditionAndVersion installed = request.getInstalled();
        if (installed != null) {
          VersionIdentifier installedVersion = installed.getResolvedVersion();
          if (version.contains(installedVersion)) {
            resolvedVersion = installedVersion;
          }
        }
      }
      if (resolvedVersion == null) {
        resolvedVersion = getToolRepository().resolveVersion(this.tool, edition.edition(), version, this);
      }
      requested.setResolvedVersion(resolvedVersion);
    }
  }

  private void completeRequestToolPath(ToolInstallRequest request) {

    Path toolPath = request.getToolPath();
    if (toolPath == null) {
      toolPath = getToolPath();
      request.setToolPath(toolPath);
    }
  }

  /**
   * @return the {@link Path} where the tool is located (installed). Will be {@code null} for global tools that do not know the {@link Path} since it is
   *     determined by the installer.
   */
  public Path getToolPath() {
    return null;
  }

  /**
   * This method is called after a tool was requested to be installed or updated.
   *
   * @param request {@code true} the {@link ToolInstallRequest}.
   */
  protected void postInstall(ToolInstallRequest request) {

    if (!request.isAlreadyInstalled()) {
      postInstallOnNewInstallation(request);
    }
  }

  /**
   * This method is called after a tool was requested to be installed or updated and a new installation was performed.
   *
   * @param request {@code true} the {@link ToolInstallRequest}.
   */
  protected void postInstallOnNewInstallation(ToolInstallRequest request) {

    // nothing to do by default
  }

  /**
   * @param edition the {@link #getInstalledEdition() edition}.
   * @param version the {@link #getInstalledVersion() version}.
   * @return the {@link Path} where this tool is installed (physically) or {@code null} if not available.
   */
  protected abstract Path getInstallationPath(String edition, VersionIdentifier version);

  /**
   * @param request the {@link ToolInstallRequest}.
   * @return the existing {@link ToolInstallation}.
   */
  protected ToolInstallation createExistingToolInstallation(ToolInstallRequest request) {

    ToolEditionAndVersion installed = request.getInstalled();

    String edition = this.tool;
    VersionIdentifier resolvedVersion = VersionIdentifier.LATEST;

    if (installed != null) {
      if (installed.getEdition() != null) {
        edition = installed.getEdition().edition();
      }
      if (installed.getResolvedVersion() != null) {
        resolvedVersion = installed.getResolvedVersion();
      }
    }

    return createExistingToolInstallation(edition, resolvedVersion, request.getProcessContext(),
        request.isAdditionalInstallation());
  }

  /**
   * @param edition the {@link #getConfiguredEdition() edition}.
   * @param installedVersion the {@link #getConfiguredVersion() version}.
   * @param environmentContext the {@link EnvironmentContext}.
   * @param extraInstallation {@code true} if the {@link ToolInstallation} is an additional installation to the
   *     {@link #getConfiguredVersion() configured version} due to a conflicting version of a {@link ToolDependency}, {@code false} otherwise.
   * @return the {@link ToolInstallation}.
   */
  protected ToolInstallation createExistingToolInstallation(String edition, VersionIdentifier installedVersion, EnvironmentContext environmentContext,
      boolean extraInstallation) {

    Path installationPath = getInstallationPath(edition, installedVersion);
    return createToolInstallation(installationPath, installedVersion, false, environmentContext, extraInstallation);
  }

  /**
   * @param rootDir the {@link ToolInstallation#rootDir() top-level installation directory}.
   * @param version the installed {@link VersionIdentifier}.
   * @param newInstallation {@link ToolInstallation#newInstallation() new installation} flag.
   * @param environmentContext the {@link EnvironmentContext}.
   * @param additionalInstallation {@code true} if the {@link ToolInstallation} is an additional installation to the
   *     {@link #getConfiguredVersion() configured version} due to a conflicting version of a {@link ToolDependency}, {@code false} otherwise.
   * @return the {@link ToolInstallation}.
   */
  protected ToolInstallation createToolInstallation(Path rootDir, VersionIdentifier version, boolean newInstallation,
      EnvironmentContext environmentContext, boolean additionalInstallation) {

    Path linkDir = rootDir;
    Path binDir = rootDir;
    if (rootDir != null) {
      // on MacOS applications have a very strange structure - see JavaDoc of findLinkDir and ToolInstallation.linkDir for details.
      linkDir = getMacOsHelper().findLinkDir(rootDir, getBinaryName());
      binDir = this.context.getFileAccess().getBinPath(linkDir);
    }
    return createToolInstallation(rootDir, linkDir, binDir, version, newInstallation, environmentContext, additionalInstallation);
  }

  /**
   * @param rootDir the {@link ToolInstallation#rootDir() top-level installation directory}.
   * @param linkDir the {@link ToolInstallation#linkDir() link directory}.
   * @param binDir the {@link ToolInstallation#binDir() bin directory}.
   * @param version the installed {@link VersionIdentifier}.
   * @param newInstallation {@link ToolInstallation#newInstallation() new installation} flag.
   * @param environmentContext the {@link EnvironmentContext}.
   * @param additionalInstallation {@code true} if the {@link ToolInstallation} is an additional installation to the
   *     {@link #getConfiguredVersion() configured version} due to a conflicting version of a {@link ToolDependency}, {@code false} otherwise.
   * @return the {@link ToolInstallation}.
   */
  protected ToolInstallation createToolInstallation(Path rootDir, Path linkDir, Path binDir, VersionIdentifier version, boolean newInstallation,
      EnvironmentContext environmentContext, boolean additionalInstallation) {

    if (linkDir != rootDir) {
      assert (!linkDir.equals(rootDir));
      Path toolVersionFile = rootDir.resolve(IdeContext.FILE_SOFTWARE_VERSION);
      if (Files.exists(toolVersionFile)) {
        this.context.getFileAccess().copy(toolVersionFile, linkDir, FileCopyMode.COPY_FILE_OVERRIDE);
      }
    }
    ToolInstallation toolInstallation = new ToolInstallation(rootDir, linkDir, binDir, version, newInstallation);
    setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    return toolInstallation;
  }

  /**
   * Called if the tool {@link ToolInstallRequest#isAlreadyInstalled() is already installed in the correct edition and version} so we can skip the
   * installation.
   *
   * @param request the {@link ToolInstallRequest}.
   * @return the {@link ToolInstallation}.
   */
  protected ToolInstallation toolAlreadyInstalled(ToolInstallRequest request) {

    logToolAlreadyInstalled(request);
    cveCheck(request);
    postInstall(request);
    return createExistingToolInstallation(request);
  }

  /**
   * Log that the tool is already installed.
   *
   * @param request the {@link ToolInstallRequest}.
   */
  protected void logToolAlreadyInstalled(ToolInstallRequest request) {
    IdeSubLogger logger;
    if (request.isSilent()) {
      logger = this.context.debug();
    } else {
      logger = this.context.info();
    }
    ToolEditionAndVersion installed = request.getInstalled();
    logger.log("Version {} of tool {} is already installed", installed.getVersion(), installed.getEdition());
  }

  /**
   * Method to get the home path of the given {@link ToolInstallation}.
   *
   * @param toolInstallation the {@link ToolInstallation}.
   * @return the Path to the home of the tool
   */
  protected Path getToolHomePath(ToolInstallation toolInstallation) {
    return toolInstallation.linkDir();
  }

  /**
   * Method to set environment variables for the process context.
   *
   * @param environmentContext the {@link EnvironmentContext} where to {@link EnvironmentContext#withEnvVar(String, String) set environment variables} for
   *     this tool.
   * @param toolInstallation the {@link ToolInstallation}.
   * @param additionalInstallation {@code true} if the {@link ToolInstallation} is an additional installation to the
   *     {@link #getConfiguredVersion() configured version} due to a conflicting version of a {@link ToolDependency}, {@code false} otherwise.
   */
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    String pathVariable = EnvironmentVariables.getToolVariablePrefix(this.tool) + "_HOME";
    Path toolHomePath = getToolHomePath(toolInstallation);
    if (toolHomePath != null) {
      environmentContext.withEnvVar(pathVariable, toolHomePath.toString());
    }
    if (additionalInstallation) {
      environmentContext.withPathEntry(toolInstallation.binDir());
    }
  }

  /**
   * @return {@code true} to extract (unpack) the downloaded binary file, {@code false} otherwise.
   */
  protected boolean isExtract() {

    return true;
  }

  /**
   * Checks a version to be installed for {@link Cve}s. If at least one {@link Cve} is found, we try to find better/safer versions as alternative. If we find
   * something better, we will suggest this to the user and ask him to make his choice.
   *
   * @param request the {@link ToolInstallRequest}.
   * @return the {@link VersionIdentifier} to install. The will may be asked (unless {@code skipSuggestions} is {@code true}) and might choose a different
   *     version than the originally requested one.
   */
  protected VersionIdentifier cveCheck(ToolInstallRequest request) {

    ToolEditionAndVersion requested = request.getRequested();
    VersionIdentifier resolvedVersion = requested.getResolvedVersion();
    if (request.isCveCheckDone()) {
      return resolvedVersion;
    }
    ToolEdition toolEdition = requested.getEdition();
    GenericVersionRange allowedVersions = requested.getVersion();
    boolean requireStableVersion = true;
    if (allowedVersions instanceof VersionIdentifier vi) {
      requireStableVersion = vi.isStable();
    }
    ToolSecurity toolSecurity = this.context.getDefaultToolRepository().findSecurity(this.tool, toolEdition.edition());
    double minSeverity = IdeVariables.CVE_MIN_SEVERITY.get(context);
    ToolVulnerabilities currentVulnerabilities = toolSecurity.findCves(resolvedVersion, this.context, minSeverity);
    ToolVersionChoice currentChoice = ToolVersionChoice.ofCurrent(requested, currentVulnerabilities);
    request.setCveCheckDone();
    if (currentChoice.logAndCheckIfEmpty(this.context)) {
      return resolvedVersion;
    }
    boolean alreadyInstalled = request.isAlreadyInstalled();
    boolean directForceInstall = this.context.isForceMode() && request.isDirect();
    if (alreadyInstalled && !directForceInstall) {
      // currently for a transitive dependency it does not make sense to suggest alternative versions, since the choice is not stored anywhere,
      // and we then would ask the user again every time the tool having this dependency is started. So we only log the problem and the user needs to react
      // (e.g. upgrade the tool with the dependency that is causing this).
      this.context.interaction("Please run 'ide -f install {}' to check for update suggestions!", this.tool);
      return resolvedVersion;
    }
    ToolVersionChoice latest = null;
    ToolVulnerabilities latestVulnerabilities = currentVulnerabilities;
    ToolVersionChoice nearest = null;
    ToolVulnerabilities nearestVulnerabilities = currentVulnerabilities;
    List<VersionIdentifier> toolVersions = getVersions();
    for (VersionIdentifier version : toolVersions) {

      if (Objects.equals(version, resolvedVersion)) {
        continue; // Skip the entire iteration for resolvedVersion
      }

      if (acceptVersion(version, allowedVersions, requireStableVersion)) {
        ToolVulnerabilities newVulnerabilities = toolSecurity.findCves(version, this.context, minSeverity);
        if (newVulnerabilities.isSafer(latestVulnerabilities)) {
          // we found a better/safer version
          ToolEditionAndVersion toolEditionAndVersion = new ToolEditionAndVersion(toolEdition, version);
          if (version.isGreater(resolvedVersion)) {
            latestVulnerabilities = newVulnerabilities;
            latest = ToolVersionChoice.ofLatest(toolEditionAndVersion, latestVulnerabilities);
            nearest = null;
          } else {
            nearestVulnerabilities = newVulnerabilities;
            nearest = ToolVersionChoice.ofNearest(toolEditionAndVersion, nearestVulnerabilities);
          }
        } else if (newVulnerabilities.isSaferOrEqual(nearestVulnerabilities)) {
          if (newVulnerabilities.isSafer(nearestVulnerabilities) || version.isGreater(resolvedVersion)) {
            nearest = ToolVersionChoice.ofNearest(new ToolEditionAndVersion(toolEdition, version), newVulnerabilities);
          }
          nearestVulnerabilities = newVulnerabilities;
        }
      }
    }
    if ((latest == null) && (nearest == null)) {
      this.context.warning(
          "Could not find any other version resolving your CVEs.\nPlease keep attention to this tool and consider updating as soon as security fixes are available.");
      if (alreadyInstalled) {
        // we came here via "ide -f install ..." but no alternative is available
        return resolvedVersion;
      }
    }
    List<ToolVersionChoice> choices = new ArrayList<>();
    choices.add(currentChoice);
    boolean addSuggestions;
    if (this.context.isForceMode() && request.isDirect()) {
      addSuggestions = true;
    } else {
      List<String> skipCveFixTools = IdeVariables.SKIP_CVE_FIX.get(this.context);
      addSuggestions = !skipCveFixTools.contains(this.tool);
    }
    if (nearest != null) {
      if (addSuggestions) {
        choices.add(nearest);
      }
      nearest.logAndCheckIfEmpty(this.context);
    }
    if (latest != null) {
      if (addSuggestions) {
        choices.add(latest);
      }
      latest.logAndCheckIfEmpty(this.context);
    }
    ToolVersionChoice[] choicesArray = choices.toArray(ToolVersionChoice[]::new);
    this.context.warning(
        "Please note that by selecting an unsafe version to install, you accept the risk to be attacked.");
    ToolVersionChoice answer = this.context.question(choicesArray, "Which version do you want to install?");
    VersionIdentifier version = answer.toolEditionAndVersion().getResolvedVersion();
    requested.setResolvedVersion(version);
    return version;
  }

  private static boolean acceptVersion(VersionIdentifier version, GenericVersionRange allowedVersions, boolean requireStableVersion) {
    if (allowedVersions.isPattern() && !allowedVersions.contains(version)) {
      return false;
    } else if (requireStableVersion && !version.isStable()) {
      return false;
    }
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
   * @return {@code true} if this tool is installed, {@code false} otherwise.
   */
  public boolean isInstalled() {

    return getInstalledVersion() != null;
  }

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
   * @return the {@link com.devonfw.tools.ide.tool.repository.DefaultToolRepository#getSortedVersions(String, String, ToolCommandlet) sorted versions} of this
   *     tool.
   */
  public List<VersionIdentifier> getVersions() {
    return getToolRepository().getSortedVersions(getName(), getConfiguredEdition(), this);
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

    EnvironmentVariables variables = this.context.getVariables();
    if (destination == null) {
      //use default location
      destination = EnvironmentVariablesFiles.SETTINGS;
    }
    EnvironmentVariables settingsVariables = variables.getByType(destination.toType());
    String name = EnvironmentVariables.getToolVersionVariable(this.tool);

    toolRepository.resolveVersion(this.tool, edition, version, this); // verify that the version actually exists
    settingsVariables.set(name, version.toString(), false);
    settingsVariables.save();
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

  /**
   * @param command the binary that will be searched in the PATH e.g. docker
   * @return true if the command is available to use
   */
  protected boolean isCommandAvailable(String command) {
    return this.context.getPath().hasBinaryOnPath(command);
  }

  /**
   * @param output the raw output string from executed command e.g. 'docker version'
   * @param pattern Regular Expression pattern that filters out the unnecessary texts.
   * @return version that has been processed.
   */
  protected VersionIdentifier resolveVersionWithPattern(String output, Pattern pattern) {
    Matcher matcher = pattern.matcher(output);

    if (matcher.find()) {
      return VersionIdentifier.of(matcher.group(1));
    } else {
      return null;
    }
  }

  /**
   * @param step the {@link Step} to get {@link Step#asSuccess() success logger} from. May be {@code null}.
   * @return the {@link IdeSubLogger} from {@link Step#asSuccess()} or {@link IdeContext#success()} as fallback.
   */
  protected IdeSubLogger asSuccess(Step step) {

    if (step == null) {
      return this.context.success();
    } else {
      return step.asSuccess();
    }
  }


  /**
   * @param step the {@link Step} to get {@link Step#asError() error logger} from. May be {@code null}.
   * @return the {@link IdeSubLogger} from {@link Step#asError()} or {@link IdeContext#error()} as fallback.
   */
  protected IdeSubLogger asError(Step step) {

    if (step == null) {
      return this.context.error();
    } else {
      return step.asError();
    }
  }
}
