package com.devonfw.tools.ide.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.SystemInfo;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogger;

/**
 * Internal helper class for MacOS workarounds.
 */
public final class MacOsHelper {

  private static final Set<String> INVALID_LINK_FOLDERS = Set.of(IdeContext.FOLDER_CONTENTS,
      IdeContext.FOLDER_RESOURCES, IdeContext.FOLDER_BIN);

  private final FileAccess fileAccess;

  private final SystemInfo systemInfo;

  private final IdeLogger logger;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext} instance.
   */
  public MacOsHelper(IdeContext context) {

    this(context.getFileAccess(), context.getSystemInfo(), context);
  }

  /**
   * The constructor.
   *
   * @param fileAccess the {@link FileAccess} instance.
   * @param systemInfo the {@link SystemInfo} instance.
   * @param logger the {@link IdeLogger} instance.
   */
  public MacOsHelper(FileAccess fileAccess, SystemInfo systemInfo, IdeLogger logger) {

    super();
    this.fileAccess = fileAccess;
    this.systemInfo = systemInfo;
    this.logger = logger;
  }

  /**
   * @param rootDir the {@link Path} to the root directory.
   * @param fileAccess the {@link FileAccess} instance.
   * @param systemInfo the {@link SystemInfo} instance.
   * @param logger the {@link IdeLogger} instance.
   * @return the {@link ToolInstallation#linkDir() link directory}.
   */
  Path findLinkDir(Path rootDir) {

    if (!this.systemInfo.isMac() || Files.isDirectory(rootDir.resolve(IdeContext.FOLDER_BIN))) {
      return rootDir;
    }
    Path contentsDir = rootDir.resolve(IdeContext.FOLDER_CONTENTS);
    if (Files.isDirectory(contentsDir)) {
      return findLinkDir(contentsDir, rootDir);
    }
    Path appDir = this.fileAccess.findFirst(rootDir,
        p -> p.getFileName().toString().endsWith(".app") && Files.isDirectory(p), false);
    if (appDir != null) {
      contentsDir = appDir.resolve(IdeContext.FOLDER_CONTENTS);
      if (Files.isDirectory(contentsDir)) {
        return findLinkDir(contentsDir, rootDir);
      }
    }
    return rootDir;
  }

  private Path findLinkDir(Path contentsDir, Path rootDir) {

    this.logger.debug("Found MacOS app in {}", contentsDir);
    Path resourcesAppBin = contentsDir.resolve(IdeContext.FOLDER_RESOURCES).resolve(IdeContext.FOLDER_APP)
        .resolve(IdeContext.FOLDER_BIN);
    if (Files.isDirectory(resourcesAppBin)) {
      return resourcesAppBin.getParent();
    }
    Path linkDir = this.fileAccess.findFirst(contentsDir, this::acceptLinkDir, false);
    if (linkDir != null) {
      return linkDir;
    }
    return rootDir;
  }

  private boolean acceptLinkDir(Path path) {

    String filename = path.getFileName().toString();
    if (INVALID_LINK_FOLDERS.contains(filename) || filename.startsWith("_")) {
      return false;
    }
    if (Files.isDirectory(path.resolve(IdeContext.FOLDER_BIN))) {
      return true;
    }
    return false;
  }

}
