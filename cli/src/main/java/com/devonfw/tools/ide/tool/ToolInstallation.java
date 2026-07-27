package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * A simple container with the information about a downloaded tool.
 *
 * @param rootDir the top-level installation directory where the tool software package has been extracted to.
 * @param linkDir the installation directory to link to the software folder inside IDE_HOME. Typically, the same as {@code rootDir} but may differ (e.g. for
 *     MacOS applications).
 * @param binDir the {@link Path} relative to {@code linkDir} pointing to the directory containing the binaries that should be put on the path (typically
 *     "bin").
 * @param resolvedVersion the {@link VersionIdentifier} of the resolved tool version installed in {@code rootDir}.
 * @param newInstallation {@code true} if the tool has been newly installed, {@code false} otherwise (the tool was already installed before).
 * @param installedAsynchronously {@code true} if the installation was launched in the background and the tool is not yet available, {@code false} otherwise.
 */
public record ToolInstallation(Path rootDir, Path linkDir, Path binDir, VersionIdentifier resolvedVersion, boolean newInstallation,
    boolean installedAsynchronously) {

  /**
   * Creates a {@link ToolInstallation} with {@code installedAsynchronously} set to {@code false}.
   *
   * @param rootDir see {@link #rootDir()}.
   * @param linkDir see {@link #linkDir()}.
   * @param binDir see {@link #binDir()}.
   * @param resolvedVersion see {@link #resolvedVersion()}.
   * @param newInstallation see {@link #newInstallation()}.
   */
  public ToolInstallation(Path rootDir, Path linkDir, Path binDir, VersionIdentifier resolvedVersion, boolean newInstallation) {

    this(rootDir, linkDir, binDir, resolvedVersion, newInstallation, false);
  }

}
