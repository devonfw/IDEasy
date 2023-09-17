package com.devonfw.tools.ide.tool;

import java.nio.file.Path;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * A simple container with the information about a downloaded tool.
 *
 * @param rootDir the top-level installation directory where the tool software package has been extracted to.
 * @param linkDir the installation directory to link to the software folder inside IDE_HOME. Typically the same as
 *        {@code rootDir} but may differ (e.g. for MacOS applications).
 * @param binDir the {@link Path} relative to {@code linkDir} pointing to the directory containing the binaries that
 *        should be put on the path (typically "bin").
 * @param resolvedVersion the {@link VersionIdentifier} of the resolved tool version installed in {@code rootDir}.
 */
public record ToolInstallation(Path rootDir, Path linkDir, Path binDir, VersionIdentifier resolvedVersion) {

}
