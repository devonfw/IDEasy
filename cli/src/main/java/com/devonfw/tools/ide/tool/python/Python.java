package com.devonfw.tools.ide.tool.python;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.tool.uv.Uv;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://www.python.org/">python</a>.
 */
public class Python extends LocalToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Python(IdeContext context) {

    super(context, "python", Set.of(Tag.PYTHON));
  }

  /**
   * Installs {@code python} using the {@link Uv#installPython(Path, VersionIdentifier, ProcessContext)} method.
   * <p>
   * Unlike the base implementation, this method requires the {@link ProcessContext} to perform the installation logic specific to {@code python}.
   *
   * @param toolRepository the {@link ToolRepository}, unused in this implementation.
   * @param resolvedVersion the {@link VersionIdentifier} of the {@code python} tool to install.
   * @param installationPath the target {@link Path} where the tool should be installed.
   * @param fileAccess the {@link FileAccess} utility for file operations.
   * @param edition the edition of the tool to install, unused in this implementation.
   * @param processContext the {@link ProcessContext} required to install the Python environment.
   */
  @Override
  protected void performToolInstallation(ToolRepository toolRepository, VersionIdentifier resolvedVersion, Path installationPath, FileAccess fileAccess,
      String edition, ProcessContext processContext) {

    if (Files.exists(installationPath)) {
      fileAccess.backup(installationPath);
    }
    Path softwarePath = installationPath.getParent();
    Uv uv = this.context.getCommandletManager().getCommandlet(Uv.class);

    uv.installPython(softwarePath, resolvedVersion, processContext);
    renameVenvFolderToPython(fileAccess, softwarePath, installationPath);
    this.context.writeVersionFile(resolvedVersion, installationPath);
    createWindowsSymlinkBinFolder(fileAccess, installationPath);
    this.context.debug("Installed {} in version {} at {}", this.tool, resolvedVersion, installationPath);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("VIRTUAL_ENV", toolInstallation.rootDir().toString());
  }

  @Override
  protected boolean isIgnoreSoftwareRepo() {

    return true;
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

    Path venvPath = softwarePath.resolve(".venv");
    fileAccess.move(venvPath, installationPath, StandardCopyOption.REPLACE_EXISTING);
  }

}
