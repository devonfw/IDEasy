package com.devonfw.tools.ide.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Tracks which tool+edition+versions have already been installed (including their CVE check) during a single <code>ide</code> command so the same
 * tool+edition+version is not processed again when it is referenced both as a top-level tool and as a dependency. A tool can be installed and CVE-checked in
 * several versions within one process (e.g. as a top-level tool and as an out-of-range dependency that resolves to a different version), so both states are
 * remembered per version, not per edition (see <a href="https://github.com/devonfw/IDEasy/issues/2827">#2827</a>).
 */
public final class ToolInstallTracker {

  /** Keys {@link #installed} and {@link #cveChecked} by {@link ToolEdition}. */
  private final Map<ToolEdition, Set<String>> installed = new HashMap<>();

  /** Keys {@link #installed} and {@link #cveChecked} by {@link ToolEdition}. */
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

    record(this.installed, edition, resolvedVersion);
  }

  /**
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to check.
   * @return {@code true} if the tool edition+version was already recorded as installed during this process, {@code false} otherwise.
   */
  public boolean isInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    if (toolEditionAndVersion == null) {
      return false;
    }
    return isInstalled(toolEditionAndVersion.getEdition(), toolEditionAndVersion.getResolvedVersion());
  }

  /**
   * @param edition the {@link ToolEdition} to check.
   * @param resolvedVersion the {@link VersionIdentifier} to check.
   * @return {@code true} if the tool edition+version was already recorded as installed during this process, {@code false} otherwise.
   */
  private boolean isInstalled(ToolEdition edition, VersionIdentifier resolvedVersion) {
    return isRecorded(this.installed, edition, resolvedVersion);
  }

  /**
   * Records that the CVE check for the given tool edition+version has already been run during this process.
   *
   * @param edition the {@link ToolEdition} that has been checked.
   * @param resolvedVersion the {@link VersionIdentifier} that has been checked.
   */
  public void recordCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {

    record(this.cveChecked, edition, resolvedVersion);
  }

  /**
   * @param edition the {@link ToolEdition} to check.
   * @param resolvedVersion the {@link VersionIdentifier} to check.
   * @return {@code true} if the CVE check for the tool edition+version was already run during this process, {@code false} otherwise.
   */
  public boolean isCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {
    return isRecorded(this.cveChecked, edition, resolvedVersion);
  }

  /**
   * Records the given tool edition+version in the given state.
   *
   * @param state the state to record into ({@link #installed} or {@link #cveChecked}).
   * @param edition the {@link ToolEdition} to record.
   * @param resolvedVersion the {@link VersionIdentifier} to record.
   */
  private void record(Map<ToolEdition, Set<String>> state, ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition != null) && (resolvedVersion != null)) {
      state.computeIfAbsent(edition, e -> new HashSet<>()).add(resolvedVersion.toString());
    }
  }

  /**
   * @param state the state to look up in ({@link #installed} or {@link #cveChecked}).
   * @param edition the {@link ToolEdition} to look up.
   * @param resolvedVersion the {@link VersionIdentifier} to look up.
   * @return {@code true} if the tool edition+version is recorded in the given state, {@code false} otherwise.
   */
  private boolean isRecorded(Map<ToolEdition, Set<String>> state, ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition == null) || (resolvedVersion == null)) {
      return false;
    }
    Set<String> versions = state.get(edition);
    return (versions != null) && versions.contains(resolvedVersion.toString());
  }

}
