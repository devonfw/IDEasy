package com.devonfw.tools.ide.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.io.FileCopyMode;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.repo.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} that is installed locally into the IDE.
 */
public abstract class LocalToolCommandlet extends ToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   *        method.
   */
  public LocalToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {

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
      this.context.success("Successfully installed {} in version {} replacing previous version {}", this.tool,
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

}
