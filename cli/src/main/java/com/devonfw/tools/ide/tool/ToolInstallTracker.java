package com.devonfw.tools.ide.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.version.VersionIdentifier;

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
   * Like {@link #installed} but for tool editions whose CVE check has already been run during this process. A redundant reference (e.g. the same tool resolved
   * as a dependency of two tools) must still run the CVE check only once, even when the individual installations are not deduplicated.
   */
  private final Map<ToolEdition, Set<String>> cveChecked = new HashMap<>();

  /**
   * Records that the given tool edition+version has been installed during this process.
   *
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to record.
   */
  public void recordInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    if (toolEditionAndVersion == null) {
      return;
    }
    recordInstalled(toolEditionAndVersion.getEdition(), toolEditionAndVersion.getResolvedVersion());
  }

  /**
   * Records that the given tool edition has been installed in the given version during this process.
   *
   * @param edition the {@link ToolEdition} that has been installed.
   * @param resolvedVersion the {@link VersionIdentifier} that has been installed.
   */
  public void recordInstalled(ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition != null) && (resolvedVersion != null)) {
      installed.computeIfAbsent(edition, e -> new HashSet<>()).add(resolvedVersion.toString());
    }
  }

  /**
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to check.
   * @return {@code true} if the tool edition+version was already recorded as installed during this process, {@code false} otherwise.
   */
  public boolean isInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    return (toolEditionAndVersion != null)
        && contains(installed, toolEditionAndVersion.getEdition(), toolEditionAndVersion.getResolvedVersion());
  }

  /**
   * Records that the CVE check for the given tool edition+version has already been run during this process.
   *
   * @param edition the {@link ToolEdition} that has been checked.
   * @param resolvedVersion the {@link VersionIdentifier} that has been checked.
   */
  public void recordCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition != null) && (resolvedVersion != null)) {
      cveChecked.computeIfAbsent(edition, e -> new HashSet<>()).add(resolvedVersion.toString());
    }
  }

  /**
   * @param edition the {@link ToolEdition} to check.
   * @param resolvedVersion the {@link VersionIdentifier} to check.
   * @return {@code true} if the CVE check for the tool edition+version was already run during this process, {@code false} otherwise.
   */
  public boolean isCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {

    return contains(cveChecked, edition, resolvedVersion);
  }

  private static boolean contains(Map<ToolEdition, Set<String>> map, ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition == null) || (resolvedVersion == null)) {
      return false;
    }
    Set<String> versions = map.get(edition);
    return (versions != null) && versions.contains(resolvedVersion.toString());
  }

}
