package com.devonfw.tools.ide.tool.python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends LocalToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(Python.class);

  private static final VersionIdentifier PYTHON_MIN_VERSION = VersionIdentifier.of("3.8.2");

  /** The folder created by {@code uv venv} inside the software folder before it is renamed to the python installation. */
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

  @Override
  protected void performToolInstallation(ToolInstallRequest request, Path installationPath) {

    VersionIdentifier resolvedVersion = request.getRequested().getResolvedVersion();
    if (resolvedVersion.compareVersion(PYTHON_MIN_VERSION).isLess()) {
      throw new CliException("Python version must be at least " + this.PYTHON_MIN_VERSION);
    }

    FileAccess fileAccess = this.context.getFileAccess();
    if (Files.exists(installationPath)) {
      fileAccess.backup(installationPath);
    }
    Path softwarePath = installationPath.getParent();
    Path venvPath = softwarePath.resolve(VENV_FOLDER);

    fileAccess.delete(venvPath);

    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);

    uv.installPython(softwarePath, resolvedVersion, request.getProcessContext());
    renameVenvFolderToPython(fileAccess, softwarePath, installationPath);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    createWindowsSymlinkBinFolder(fileAccess, installationPath);
    LOG.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    environmentContext.withEnvVar("VIRTUAL_ENV", toolInstallation.rootDir().toString());
    environmentContext.withEnvVar("UV_PROJECT_ENVIRONMENT", toolInstallation.rootDir().toString());
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
  }

  @Override
  protected boolean isIgnoreMissingSoftwareVersionFile() {

    // https://github.com/devonfw/IDEasy/issues/2190
    return true;
  }

  @Override
  protected VersionIdentifier detectInstalledVersion(Path installationPath, VersionIdentifier resolvedVersion) {

    VersionIdentifier version = readVersionFromPyvenvCfg(installationPath);
    if (version == null) {
      version = readVersionFromInterpreter(installationPath);
    }
    if (version == null) {
      LOG.warn("Could not detect the installed version of python at {} - assuming {}.", installationPath, resolvedVersion);
      return resolvedVersion;
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
    Path binaryPath = binPath.resolve(getBinaryName());
    if (!Files.exists(binaryPath)) {
      binaryPath = binPath.resolve(getBinaryName() + ".exe");
    }
    if (!Files.exists(binaryPath)) {
      LOG.debug("Python binary does not exist in {}.", binPath);
      return null;
    }
    String output = this.context.newProcess().runAndGetSingleOutput(IdeLogLevel.DEBUG, binaryPath.toString(), "--version");
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
    LOG.debug("Could not parse version from output '{}' of {}.", output, binaryPath);
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

  /**
   * Renames the ".venv" folder into the installation path (Python).
   *
   * @param fileAccess the {@link FileAccess} utility for file operations.
   * @param softwarePath the path where the software is installed.
   * @param installationPath the target path where the ".venv" folder should be moved.
   */
  private void renameVenvFolderToPython(FileAccess fileAccess, Path softwarePath, Path installationPath) {

    Path venvPath = softwarePath.resolve(VENV_FOLDER);
    fileAccess.move(venvPath, installationPath, StandardCopyOption.REPLACE_EXISTING);
  }

}
