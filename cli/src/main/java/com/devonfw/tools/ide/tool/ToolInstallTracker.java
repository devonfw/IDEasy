package com.devonfw.tools.ide.tool;

import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Tracks which tools have already been installed (including their CVE check) during a single <code>ide update</code> / <code>ide create</code> process so the
 * same tool+version is not processed again when it is referenced both as a top-level tool and as a dependency. Versions are stored as strings because
 * {@link VersionIdentifier} uses identity-based {@code equals}.
 */
public final class ToolInstallTracker {

  private final Map<ToolEdition, Entry> state = new HashMap<>();

  /** The installation state of a single {@link ToolEdition} during the current process. */
  private static final class Entry {

    /** {@code true} if this edition has been installed during the current process. */
    boolean installed;

    /** The version installed, resolved before the CVE check (what a redundant reference resolves back to). */
    String requestedVersion;

    /** {@code true} if the CVE check for this edition has been run during the current process. */
    boolean cveChecked;

    /** The version actually CVE-checked; may differ from {@link #requestedVersion} if the CVE check bumped it. */
    String checkedVersion;

  }

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
      Entry entry = entryFor(edition);
      entry.installed = true;
      entry.requestedVersion = resolvedVersion.toString();
    }
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

    Entry entry = entry(edition);
    return (entry != null) && entry.installed && entry.requestedVersion != null
        && entry.requestedVersion.equals(resolvedVersion.toString());
  }

  /**
   * Records that the CVE check for the given tool edition+version has already been run during this process.
   *
   * @param edition the {@link ToolEdition} that has been checked.
   * @param resolvedVersion the {@link VersionIdentifier} that has been checked.
   */
  public void recordCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition != null) && (resolvedVersion != null)) {
      Entry entry = entryFor(edition);
      entry.cveChecked = true;
      entry.checkedVersion = resolvedVersion.toString();
    }
  }

  /**
   * @param edition the {@link ToolEdition} to check.
   * @param resolvedVersion the {@link VersionIdentifier} to check.
   * @return {@code true} if the CVE check for the tool edition+version was already run during this process, {@code false} otherwise.
   */
  public boolean isCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {

    if ((edition == null) || (resolvedVersion == null)) {
      return false;
    }
    Entry entry = entry(edition);
    return (entry != null) && entry.cveChecked && entry.checkedVersion != null
        && entry.checkedVersion.equals(resolvedVersion.toString());
  }

  private Entry entryFor(ToolEdition edition) {
    return this.state.computeIfAbsent(edition, e -> new Entry());
  }

  private Entry entry(ToolEdition edition) {
    return (edition == null) ? null : this.state.get(edition);
  }

}
