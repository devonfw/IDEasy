package com.devonfw.tools.ide.tool.python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.AbstractToolCommandlet;
import com.devonfw.tools.ide.tool.ToolEdition;
import com.devonfw.tools.ide.tool.ToolEditionAndVersion;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.variable.IdeVariables;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link AbstractToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends AbstractLocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Python.class);

  private static final VersionIdentifier PYTHON_MIN_VERSION = VersionIdentifier.ofRequired("3.8.2");

  /** The per-project folder (inside {@code IDE_HOME}) that holds the virtual environment for the project's python packages. */
  static final String VENV_FOLDER = ".venv";

  private static final String FILE_PYVENV_CFG = "pyvenv.cfg";

  private static final String PYVENV_CFG_VERSION_INFO = "version_info";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Python(IdeContext context) {

    super(context, "python", Set.of(Tag.PYTHON));
  }

  /**
   * Ensures the tool dependencies required to resolve the Python version are installed before the version is resolved.
   * <p>
   * Resolving the Python version uses {@code uv python list} and therefore requires {@code uv} already. The standard dependency installation
   * ({@code installToolDependencies}) runs only after the version has been resolved, which is too late for that. The required dependencies (the {@code uv}
   * version range) are not hard-coded but read from the {@code dependencies.json} of {@code ide-urls}, so the required {@code uv} version can be adjusted per
   * Python version and fixed in emergencies without publishing a new IDEasy release.
   *
   * @param request the {@link ToolInstallRequest} to complete.
   */
  @Override
  protected void completeRequest(ToolInstallRequest request) {

    ToolEditionAndVersion requested = request.getRequested();
    if (requested == null) {
      // 'installAsDependency' logs the requested edition of this request, so make sure it is set before installing the dependencies.
      request.setRequested(new ToolEditionAndVersion(new ToolEdition(this.tool, getConfiguredEdition())));
    } else if (requested.getEdition() == null) {
      requested.setEdition(new ToolEdition(this.tool, getConfiguredEdition()));
    }
    VersionIdentifier version = request.getRequested().getResolvedVersion();
    if (version == null) {
      // No explicit version is known (yet). Every version that will be resolved later is at least the minimum supported one, so use the configured or at
      // least the minimum supported version to determine the required dependencies.
      version = (getConfiguredVersion() != null) ? getConfiguredVersion() : PYTHON_MIN_VERSION;
    }
    Collection<ToolDependency> dependencies = getToolRepository().findDependencies(this.tool, getConfiguredEdition(), version);
    for (ToolDependency dependency : dependencies) {
      AbstractLocalToolCommandlet dependencyTool = this.context.getCommandletManager().getRequiredLocalToolCommandlet(dependency.tool());
      dependencyTool.installAsDependency(dependency.versionRange(), request);
    }
    super.completeRequest(request);
  }

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    if (resolvedVersion.compareVersion(PYTHON_MIN_VERSION).isLess()) {
      throw new CliException("Python version must be at least " + this.PYTHON_MIN_VERSION);
    }

    // Python is installed as a pristine, versioned interpreter into the shared software repository (see getInstallationPath). uv lays the interpreter out in a
    // nested "cpython-«version»-«platform»" folder plus management files, so we install into a scratch directory and move the interpreter folder into place.
    // Unlike a virtual environment, a pristine interpreter keeps its executable in the installation root (not in a "Scripts" folder on Windows), so no "bin"
    // symlink is created here (see createWindowsSymlinkBinFolder, which is only applied to the per-project virtual environment in setupProjectEnvironment).
    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.exists(installationPath)) {
      fileAccess.backup(installationPath);
    }

    Path scratchDir = fileAccess.createTempDir("python-install");
    try {
      Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);
      uv.installInterpreter(scratchDir, resolvedVersion, request.getProcessContext());

      Path interpreterDir = findInterpreterDir(scratchDir, resolvedVersion);
      fileAccess.mkdirs(installationPath.getParent());
      fileAccess.move(interpreterDir, installationPath, StandardCopyOption.REPLACE_EXISTING);
      this.context.writeVersionFile(resolvedVersion, installationPath);
      LOG.debug("Installed pristine {} in version {} at {}", this.tool, resolvedVersion, installationPath);
    } finally {
      fileAccess.delete(scratchDir);
    }
  }

  /**
   * Locates the {@code cpython-«version»} interpreter folder created by {@code uv python install} inside the given scratch directory.
   *
   * @param scratchDir the directory passed to {@code uv python install --install-dir}
   * @param version the {@link VersionIdentifier} that was installed
   * @return the {@link Path} of the interpreter folder.
   */
  private Path findInterpreterDir(Path scratchDir, VersionIdentifier version) {

    Path result = this.context.getFileAccess().findFirst(scratchDir, p -> {
      String name = p.getFileName().toString();
      return name.startsWith("cpython-" + version + "-");
    }, false);
    if (result == null) {
      throw new CliException("Could not find the python interpreter installed by uv in " + scratchDir);
    }
    return result;
  }

  /**
   * Places the pristine interpreter in the shared software repository under the {@link ToolRepository#ID_DEFAULT default} namespace
   * ({@code $IDE_ROOT/software/default/python/python/«version»}) - the same layout the default repository produces for other pristine, versioned tools (e.g.
   * node). Version resolution itself still uses {@link PythonRepository} (see {@link #getToolRepository()}), which is only consulted to list available
   * versions via {@code uv}; it is not a download source, so the pristine interpreter must not be stored under its {@link PythonRepository#ID ID}.
   *
   * @param edition the {@link #getConfiguredEdition() tool edition}.
   * @param resolvedVersion the resolved {@link VersionIdentifier version}.
   * @return the {@link Path} where the pristine interpreter is (to be) installed.
   */
  @Override
  protected Path getInstallationPath(String edition, VersionIdentifier resolvedVersion) {

    Path softwareRepositoryPath = this.context.getSoftwareRepositoryPath();
    if (softwareRepositoryPath == null) {
      return null;
    }
    return softwareRepositoryPath.resolve(ToolRepository.ID_DEFAULT).resolve(this.tool).resolve(edition).resolve(resolvedVersion.toString());
  }

  /**
   * @return the {@link Path} to the per-project virtual environment (where the project's python packages live) or {@code null} if not inside a project.
   * A stable per-project location (inside {@code IDE_HOME} by default) so the virtual environment can be created, updated and removed by IDEasy. The user may
   * relocate it via {@link IdeVariables#PYTHON_VENV_DIR}.
   */
  Path getVenvPath() {

    Path ideHome = this.context.getIdeHome();
    if (ideHome == null) {
      return null;
    }
    String venvDir = IdeVariables.PYTHON_VENV_DIR.get(this.context);
    if (venvDir != null && !venvDir.isBlank()) {
      return Path.of(venvDir);
    }
    return ideHome.resolve(VENV_FOLDER);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    // The python packages of the project (pip / ruff / ...) live in a per-project virtual environment so that two projects on the same python interpreter
    // don't collide (see #352). The pristine interpreter itself stays shared in the software repository.
    Path venvPath = getVenvPath();
    if (venvPath == null) {
      // running in global mode (no project) - there is no per-project virtual environment to point to
      return;
    }
    environmentContext.withEnvVar("VIRTUAL_ENV", venvPath.toString());
    environmentContext.withEnvVar("UV_PROJECT_ENVIRONMENT", venvPath.toString());
    environmentContext.withPathEntry(venvPath.resolve("bin"));
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {

    super.postInstall(request);
    setupProjectEnvironment(request);
  }

  /**
   * Removes the per-project virtual environment (see {@link #setupProjectEnvironment}) in addition to the per-project tool link (deleted by the default
   * implementation, which points at the shared pristine interpreter and must not be followed). This leaves nothing behind inside the project on uninstall (see
   * <a href="https://github.com/devonfw/IDEasy/issues/2561">#2561</a>).
   *
   * @param toolPath the current {@link #getToolPath() tool path} ({@code software/python}, the per-project link to the shared pristine interpreter).
   */
  @Override
  protected void performUninstall(Path toolPath) {

    super.performUninstall(toolPath);
    Path venvPath = getVenvPath();
    if (venvPath != null) {
      this.context.getFileAccess().delete(venvPath);
    }
  }

  /**
   * Creates the per-project virtual environment (backed by the pristine interpreter). Only done inside a project (a pristine interpreter is shared and must not
   * be modified).
   *
   * @param request the {@link ToolInstallRequest}.
   */
  private void setupProjectEnvironment(ToolInstallRequest request) {

    Path ideHome = this.context.getIdeHome();
    if (ideHome == null) {
      return;
    }
    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);

    // create the per-project virtual environment backed by the pristine interpreter (honoring a relocated location via PYTHON_VENV_DIR, see getVenvPath)
    Path venvPath = getVenvPath();
    Path interpreterDir = getInstallationPath(getConfiguredEdition(), resolvedVersion);
    if (interpreterDir != null) {
      uv.createVirtualEnvironment(venvPath, interpreterDir, request.getProcessContext());
      // uv lays the executables out in a "Scripts" folder on Windows - expose them as "bin" so the PATH entry set in setEnvironment resolves on all platforms
      createWindowsSymlinkBinFolder(this.context.getFileAccess(), venvPath);
    }
  }

  @Override
  protected boolean isIgnoreMissingSoftwareVersionFile() {

    // https://github.com/devonfw/IDEasy/issues/2190
    return true;
  }

  @Override
  protected VersionIdentifier computeInstalledVersionFromLocalSoftwareFolder() {

    Path toolPath = getToolPath();
    VersionIdentifier version = readVersionFromPyvenvCfg(toolPath);
    if (version == null) {
      version = readVersionFromInterpreter(toolPath);
    }
    if (version != null) {
      LOG.debug("Determined version {} of python from the installation at {}.", version, toolPath);
    }
    return version;
  }

  /**
   * @param installationPath the {@link Path} to the Python installation.
   * @return the {@link VersionIdentifier} from the {@code version_info} entry of {@code pyvenv.cfg} or {@code null} if not available or not precise enough.
   */
  private VersionIdentifier readVersionFromPyvenvCfg(Path installationPath) {

    Path pyvenvCfg = installationPath.resolve(FILE_PYVENV_CFG);
    if (!Files.exists(pyvenvCfg)) {
      return null;
    }
    String content = this.context.getFileAccess().readFileContent(pyvenvCfg);
    for (String line : content.split("\\R")) {
      String[] keyAndValue = line.split("=", 2);
      if ((keyAndValue.length == 2) && keyAndValue[0].trim().equals(PYVENV_CFG_VERSION_INFO)) {
        String value = keyAndValue[1].trim();
        // uv only writes the minor version (e.g. "3.13") for its own interpreters what is too imprecise for us
        if (value.chars().filter(c -> c == '.').count() >= 2) {
          return VersionIdentifier.ofRequired(value);
        }
        LOG.debug("Ignoring imprecise version {} from {}.", value, pyvenvCfg);
      }
    }
    return null;
  }

  /**
   * @param installationPath the {@link Path} to the Python installation.
   * @return the {@link VersionIdentifier} reported by the installed python interpreter or {@code null} if it could not be determined.
   */
  private VersionIdentifier readVersionFromInterpreter(Path installationPath) {

    Path binPath = this.context.getFileAccess().getBinPath(installationPath);
    Path binaryPath = this.context.getPath().findBinary(binPath.resolve(getBinaryName()));
    if (!Files.exists(binaryPath)) {
      LOG.debug("Python binary does not exist in {}.", binPath);
      return null;
    }
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.NONE).withPathEntry(binPath);
    ProcessResult result = runTool(pc, ProcessMode.DEFAULT_CAPTURE, List.of("--version"));
    String output = result.getSingleOutput(IdeLogLevel.DEBUG);
    if (output == null) {
      return null;
    }
    String version = output.trim();
    int lastSpace = version.lastIndexOf(' ');
    if (lastSpace >= 0) {
      version = version.substring(lastSpace + 1);
    }
    if (!version.isEmpty() && Character.isDigit(version.charAt(0))) {
      return VersionIdentifier.ofRequired(version);
    }
    LOG.debug("Could not parse version from output '{}' of {}.", output, binPath);
    return null;
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getPythonRepository();
  }

  /**
   * On Windows systems {@code uv} lays out a virtual environment with its executables in a {@code Scripts} folder. This creates a symlink from that
   * {@code Scripts} folder to a {@code bin} folder so that the {@code bin} path used to resolve the executable (see {@link #setEnvironment}) exists on all
   * platforms. Does nothing on non-Windows systems, where {@code uv} already uses a {@code bin} folder.
   *
   * @param fileAccess the {@link FileAccess} utility for file operations.
   * @param installationPath the root of the Python virtual environment.
   */
  private void createWindowsSymlinkBinFolder(FileAccess fileAccess, Path installationPath) {

    if (!this.context.getSystemInfo().isWindows()) {
      return;
    }
    Path scriptsPath = installationPath.resolve("Scripts");
    Path binPath = installationPath.resolve("bin");
    fileAccess.symlink(scriptsPath, binPath);
  }

}
