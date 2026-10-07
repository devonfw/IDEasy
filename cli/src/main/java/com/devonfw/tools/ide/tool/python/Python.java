package com.devonfw.tools.ide.tool.python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link AbstractToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends AbstractLocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Python.class);

  private static final VersionIdentifier PYTHON_MIN_VERSION = VersionIdentifier.of("3.8.2");

  /** The per-project folder (inside {@code IDE_HOME}) that holds the {@code uv} tool store (e.g. {@code ruff}). */
  static final String UV_TOOLS_FOLDER = ".uv-tools";

  /** The per-project folder (inside {@code IDE_HOME}) that holds the virtual environment for the project's python packages. */
  static final String VENV_FOLDER = ".venv";

  /** The tool primed into the per-project {@code uv} tool store on installation so that it is available immediately. */
  private static final String RUFF_TOOL = "ruff";

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

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    if (resolvedVersion.compareVersion(PYTHON_MIN_VERSION).isLess()) {
      throw new CliException("Python version must be at least " + this.PYTHON_MIN_VERSION);
    }

    // Python is installed as a pristine, versioned interpreter into the shared software repository (see getInstallationPath). uv lays the interpreter out in a
    // nested "cpython-«version»-«platform»" folder plus management files, so we install into a scratch directory and move the interpreter folder into place.
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
      createWindowsSymlinkBinFolder(fileAccess, installationPath);
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
      return name.startsWith("cpython-" + version) && !name.equals(version.toString());
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

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    Path ideHome = this.context.getIdeHome();
    if (ideHome == null) {
      // running in global mode (no project) - there is no per-project virtual environment to point to
      return;
    }
    // The python packages of the project (pip / ruff / ...) live in a per-project virtual environment so that two projects on the same python interpreter
    // don't collide (see #352). The pristine interpreter itself stays shared in the software repository.
    Path venvPath = ideHome.resolve(VENV_FOLDER);
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
   * Creates the per-project virtual environment (backed by the pristine interpreter) and primes {@code ruff} into the per-project {@code uv} tool store. Only
   * done inside a project (a pristine interpreter is shared and must not be modified).
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
    Path uvToolDir = ideHome.resolve(UV_TOOLS_FOLDER);

    // create the per-project virtual environment backed by the pristine interpreter
    Path venvPath = ideHome.resolve(VENV_FOLDER);
    Path interpreterDir = getInstallationPath(getConfiguredEdition(), resolvedVersion);
    if (interpreterDir != null) {
      uv.createVirtualEnvironment(venvPath, interpreterDir, request.getProcessContext());
    }

    // prime ruff into the per-project uv tool store (best effort - a failure must not fail the python installation)
    ProcessContext processContext = request.getProcessContext().withEnvVar("UV_TOOL_DIR", uvToolDir.toString())
        .withEnvVar("UV_TOOL_BIN_DIR", uvToolDir.resolve("bin").toString());
    uv.installTool(RUFF_TOOL, processContext);
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
   * @param installationPath the {@link Path} to the virtual environment.
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
          return VersionIdentifier.of(value);
        }
        LOG.debug("Ignoring imprecise version {} from {}.", value, pyvenvCfg);
      }
    }
    return null;
  }

  /**
   * @param installationPath the {@link Path} to the virtual environment.
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
      return VersionIdentifier.of(version);
    }
    LOG.debug("Could not parse version from output '{}' of {}.", output, binPath);
    return null;
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getPythonRepository();
  }

  /**
   * Creates a symlink from the "Scripts" folder to the "bin" folder on Windows systems. This is necessary for compatibility with tools that expect a "bin"
   * directory.
   *
   * @param fileAccess the {@link FileAccess} utility for file operations.
   * @param installationPath the path where Python is installed.
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
