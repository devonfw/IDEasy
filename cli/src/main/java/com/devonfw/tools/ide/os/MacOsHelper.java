package com.devonfw.tools.ide.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.repository.ToolRepository;

/**
 * Internal helper class for MacOS workarounds.
 */
public final class MacOsHelper {

  private static final Logger LOG = LoggerFactory.getLogger(MacOsHelper.class);

  private static final Set<String> INVALID_LINK_FOLDERS = Set.of(IdeContext.FOLDER_CONTENTS,
      IdeContext.FOLDER_RESOURCES, IdeContext.FOLDER_BIN);

  private final IdeContext context;

  private final FileAccess fileAccess;

  private final SystemInfo systemInfo;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext} instance.
   */
  public MacOsHelper(IdeContext context) {

    super();
    this.context = context;
    this.fileAccess = context.getFileAccess();
    this.systemInfo = context.getSystemInfo();
  }

  /**
   * The constructor.
   *
   * @param fileAccess the {@link FileAccess} instance.
   * @param systemInfo the {@link SystemInfo} instance.
   */
  public MacOsHelper(FileAccess fileAccess, SystemInfo systemInfo) {

    super();
    this.context = null;
    this.fileAccess = fileAccess;
    this.systemInfo = systemInfo;
  }

  /**
   * Fixes macOS Gatekeeper blocking for downloaded tools. On macOS 15.1+ (Apple Silicon), just removing {@code com.apple.quarantine} is not enough since
   * unsigned apps still get the "is damaged" error. So we clear all xattrs first, then ad-hoc codesign any {@code .app} bundles. Call this after writing
   * {@code .ide.software.version} since codesigning seals the bundle.
   *
   * @param path the {@link Path} to the installation directory.
   */
  public void removeQuarantineAttribute(Path path) {

    if (!this.systemInfo.isMac()) {
      return;
    }
    if (this.context == null) {
      LOG.debug("Cannot fix Gatekeeper for {} - no context available", path);
      return;
    }
    // clear all extended attributes (quarantine, resource forks, etc.)
    LOG.debug("Clearing extended attributes from {}", path);
    try {
      this.context.newProcess().executable("xattr").addArgs("-cr", path).run(ProcessMode.DEFAULT_SILENT);
    } catch (Exception e) {
      LOG.warn("Could not clear extended attributes from {}: {}", path, e.getMessage(), e);
    }
    // ad-hoc codesign .app bundles only if they are not already properly signed (e.g. Eclipse is notarized - we must not replace that)
    Path appDir = findAppDir(path);
    if (appDir != null) {
      try {
        ProcessResult verifyResult = this.context.newProcess().executable("codesign")
            .addArgs("-v", appDir).run(ProcessMode.DEFAULT_SILENT);
        if (!verifyResult.isSuccessful()) {
          LOG.debug("Ad-hoc codesigning {}", appDir);
          ProcessResult signResult = this.context.newProcess().executable("codesign")
              .addArgs("--force", "--deep", "--sign", "-", appDir).run(ProcessMode.DEFAULT_SILENT);
          if (!signResult.isSuccessful()) {
            LOG.warn("Could not codesign {} - app may be blocked by Gatekeeper", appDir);
          }
        }
      } catch (Exception e) {
        LOG.warn("Codesign not available for {}: {}", appDir, e.getMessage());
      }
    }
  }

  /**
   * @param rootDir the {@link Path} to the root directory.
   * @return the path to the app directory.
   */
  public Path findAppDir(Path rootDir) {
    return this.fileAccess.findFirst(rootDir,
        p -> p.getFileName().toString().endsWith(".app") && Files.isDirectory(p), false);
  }

  /**
   * Finds the directory containing the tool's executables inside a macOS {@code .app} bundle, without requiring the binary name. This method exists separately
   * from {@link #findLinkDir(Path, String)} because it is called from {@code getToolBinPath()}, which is itself called by {@code getBinaryName()} — using
   * {@code findLinkDir} there would cause infinite recursion since it requires the binary name.
   *
   * @param rootDir the {@link Path} to the root directory that may contain a {@code .app} bundle.
   * @return the binary directory inside the {@code .app} bundle, or {@code rootDir} if not a {@code .app} structure.
   */
  public Path findBinDir(Path rootDir) {

    if (!this.systemInfo.isMac() || Files.isDirectory(rootDir.resolve(IdeContext.FOLDER_BIN))) {
      return rootDir;
    }
    Path contentsDir = rootDir.resolve(IdeContext.FOLDER_CONTENTS);
    if (!Files.isDirectory(contentsDir)) {
      Path appDir = findAppDir(rootDir);
      if (appDir == null) {
        return rootDir;
      }
      contentsDir = appDir.resolve(IdeContext.FOLDER_CONTENTS);
      if (!Files.isDirectory(contentsDir)) {
        return rootDir;
      }
    }
    Path resourcesApp = contentsDir.resolve(IdeContext.FOLDER_RESOURCES).resolve(IdeContext.FOLDER_APP);
    if (Files.isDirectory(resourcesApp)) {
      return resourcesApp;
    }
    Path macosDir = contentsDir.resolve("MacOS");
    if (Files.isDirectory(macosDir)) {
      return macosDir;
    }
    return rootDir;
  }

  /**
   * @param rootDir the {@link Path} to the root directory.
   * @param tool the name of the tool to find the link directory for.
   * @return the {@link com.devonfw.tools.ide.tool.ToolInstallation#linkDir() link directory}.
   */
  public Path findLinkDir(Path rootDir, String tool) {

    if (!this.systemInfo.isMac() || Files.isDirectory(rootDir.resolve(IdeContext.FOLDER_BIN))) {
      return rootDir;
    }
    Path contentsDir = rootDir.resolve(IdeContext.FOLDER_CONTENTS);
    if (Files.isDirectory(contentsDir)) {
      return findLinkDir(contentsDir, rootDir, tool);
    }
    Path appDir = findAppDir(rootDir);
    if (appDir != null) {
      contentsDir = appDir.resolve(IdeContext.FOLDER_CONTENTS);
      if (Files.isDirectory(contentsDir)) {
        return findLinkDir(contentsDir, rootDir, tool);
      }
    }
    return rootDir;
  }

  /**
   * Finds the root tool path of a tool in MacOS
   *
   * @param commandlet the {@link ToolCommandlet}
   * @param context the {@link IdeContext}
   * @return a {@link String}
   */
  public Path findRootToolPath(ToolCommandlet commandlet, IdeContext context) {
    return context.getSoftwareRepositoryPath().resolve(ToolRepository.ID_DEFAULT).resolve(commandlet.getName())
        .resolve(commandlet.getInstalledEdition())
        .resolve(commandlet.getInstalledVersion().toString());
  }

  private Path findLinkDir(Path contentsDir, Path rootDir, String tool) {

    LOG.debug("Found MacOS app in {}", contentsDir);
    Path resourcesAppBin = contentsDir.resolve(IdeContext.FOLDER_RESOURCES).resolve(IdeContext.FOLDER_APP)
        .resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(resourcesAppBin)) {
      return resourcesAppBin.getParent();
    }
    Path linkDir = findContentSubfolder(contentsDir, tool);
    if (linkDir != null) {
      return linkDir;
    }
    return rootDir;
  }

  private Path findContentSubfolder(Path dir, String tool) {

    try (Stream<Path> childStream = Files.list(dir)) {
      Iterator<Path> iterator = childStream.iterator();
      while (iterator.hasNext()) {
        Path child = iterator.next();
        String filename = child.getFileName().toString();
        if (INVALID_LINK_FOLDERS.contains(filename) || filename.startsWith("_")) {
          continue;
        } else if (Files.isDirectory(child.resolve(IdeContext.FOLDER_BIN)) || Files.exists(child.resolve(tool))) {
          return child;
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to search for file in " + dir, e);
    }
    return null;
  }

}
