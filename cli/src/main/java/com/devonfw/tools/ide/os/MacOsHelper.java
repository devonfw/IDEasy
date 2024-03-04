package com.devonfw.tools.ide.os;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.io.FileAccess;
import com.devonfw.tools.ide.log.IdeLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

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
    Path appDir = this.fileAccess.findFirst(rootDir,
        p -> p.getFileName().toString().endsWith(".app") && Files.isDirectory(p), false);
    if (appDir != null) {
      contentsDir = appDir.resolve(IdeContext.FOLDER_CONTENTS);
      if (Files.isDirectory(contentsDir)) {
        return findLinkDir(contentsDir, rootDir, tool);
      }
    }
    return rootDir;
  }

  private Path findLinkDir(Path contentsDir, Path rootDir, String tool) {

    this.logger.debug("Found MacOS app in {}", contentsDir);
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
