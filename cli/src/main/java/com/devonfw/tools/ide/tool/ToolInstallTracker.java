package com.devonfw.tools.ide.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks which tools have already been installed (including their CVE check) during a single <code>ide update</code> / <code>ide create</code> process so the
 * same tool+version is not processed again when it is referenced both as a top-level tool and as a dependency.
 */
public final class ToolInstallTracker {

  /**
   * Keyed by {@link ToolEdition} (a value-based record) and version string. The version is stored as its string form because
   * {@link com.devonfw.tools.ide.version.VersionIdentifier} uses identity-based {@code equals} and the same version string may be represented by different
   * instances.
   */
  private final Map<ToolEdition, Set<String>> installed = new HashMap<>();

  /**
   * Records that the given tool edition+version has been installed during this process.
   *
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to record.
   */
  public void recordInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    if ((toolEditionAndVersion == null) || (toolEditionAndVersion.getEdition() == null)
        || (toolEditionAndVersion.getResolvedVersion() == null)) {
      return;
    }
    installed.computeIfAbsent(toolEditionAndVersion.getEdition(), edition -> new HashSet<>())
        .add(toolEditionAndVersion.getResolvedVersion().toString());
  }

  /**
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to check.
   * @return {@code true} if the tool edition+version was already recorded as installed during this process, {@code false} otherwise.
   */
  public boolean isInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    if ((toolEditionAndVersion == null) || (toolEditionAndVersion.getEdition() == null)
        || (toolEditionAndVersion.getResolvedVersion() == null)) {
      return false;
    }
    Set<String> versions = installed.get(toolEditionAndVersion.getEdition());
    return (versions != null) && versions.contains(toolEditionAndVersion.getResolvedVersion().toString());
  }

}
