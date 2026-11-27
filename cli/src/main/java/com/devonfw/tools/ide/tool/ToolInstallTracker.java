package com.devonfw.tools.ide.tool;

import java.util.HashMap;
import java.util.Map;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Tracks which tool+edition+versions have already been installed (including their CVE check) during a single <code>ide</code> command so the same
 * tool+edition+version is not processed again when it is referenced both as a top-level tool and as a dependency. A tool can be installed and CVE-checked in
 * several versions within one process (e.g. as a top-level tool and as an out-of-range dependency that resolves to a different version), so both states are
 * remembered per version, not per edition (see <a href="https://github.com/devonfw/IDEasy/issues/2827">#2827</a>).
 */
public final class ToolInstallTracker {

  /** Maps {@link ToolEditionAndVersion} to {@link ToolState}. */
  private final Map<ToolEditionAndVersion, ToolState> state = new HashMap<>();

  /**
   * Records that the given tool edition+version has been installed during this process.
   *
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to record.
   */
  public void recordInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    if (toolEditionAndVersion == null) {
      return;
    }
    // we need to disassemble as the configured and resolved version may differ causing equals mismatch
    recordInstalled(toolEditionAndVersion.getEdition(), toolEditionAndVersion.getResolvedVersion());
  }

  /**
   * Records that the given tool edition has been installed in the given version during this process.
   *
   * @param edition the {@link ToolEdition} that has been installed.
   * @param resolvedVersion the {@link VersionIdentifier} that has been installed.
   */
  public void recordInstalled(ToolEdition edition, VersionIdentifier resolvedVersion) {

    record(edition, resolvedVersion, ToolState.INSTALLED);
  }

  /**
   * @param toolEditionAndVersion the {@link ToolEditionAndVersion} to check.
   * @return {@code true} if the tool edition+version was already recorded as installed during this process, {@code false} otherwise.
   */
  public boolean isInstalled(ToolEditionAndVersion toolEditionAndVersion) {

    if (toolEditionAndVersion == null) {
      return false;
    }
    // we need to disassemble as the configured and resolved version may differ causing equals mismatch
    return isInstalled(toolEditionAndVersion.getEdition(), toolEditionAndVersion.getResolvedVersion());
  }

  /**
   * @param edition the {@link ToolEdition} to check.
   * @param resolvedVersion the {@link VersionIdentifier} to check.
   * @return {@code true} if the tool edition+version was already recorded as installed during this process, {@code false} otherwise.
   */
  private boolean isInstalled(ToolEdition edition, VersionIdentifier resolvedVersion) {
    return isRecorded(edition, resolvedVersion, ToolState.INSTALLED);
  }

  /**
   * Records that the CVE check for the given tool edition+version has already been run during this process.
   *
   * @param edition the {@link ToolEdition} that has been checked.
   * @param resolvedVersion the {@link VersionIdentifier} that has been checked.
   */
  public void recordCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {

    record(edition, resolvedVersion, ToolState.CVE_CHECKED);
  }

  /**
   * @param edition the {@link ToolEdition} to check.
   * @param resolvedVersion the {@link VersionIdentifier} to check.
   * @return {@code true} if the CVE check for the tool edition+version was already run during this process, {@code false} otherwise.
   */
  public boolean isCveChecked(ToolEdition edition, VersionIdentifier resolvedVersion) {
    return isRecorded(edition, resolvedVersion, ToolState.CVE_CHECKED);
  }

  /**
   * Records the given tool edition+version in the given state.
   *
   * @param edition the {@link ToolEdition} to record.
   * @param resolvedVersion the {@link VersionIdentifier} to record.
   * @param state the {@link ToolState}.
   */
  private void record(ToolEdition edition, VersionIdentifier resolvedVersion, ToolState state) {

    if ((edition != null) && (resolvedVersion != null)) {
      ToolEditionAndVersion key = new ToolEditionAndVersion(edition, resolvedVersion);
      if ((state == ToolState.CVE_CHECKED) && this.state.get(key) == ToolState.INSTALLED) {
        return; // INSTALLED implies CVE_CHECKED
      }
      this.state.put(key, state);
    }
  }

  /**
   * @param edition the {@link ToolEdition} to look up.
   * @param resolvedVersion the {@link VersionIdentifier} to look up.
   * @param state the {@link ToolState}.
   * @return {@code true} if the tool edition+version is recorded in the given state, {@code false} otherwise.
   */
  private boolean isRecorded(ToolEdition edition, VersionIdentifier resolvedVersion, ToolState state) {

    if ((edition == null) || (resolvedVersion == null)) {
      return false;
    }
    ToolEditionAndVersion key = new ToolEditionAndVersion(edition, resolvedVersion);
    ToolState recordedState = this.state.get(key);
    return (recordedState == state) || ((recordedState == ToolState.INSTALLED) && (state == ToolState.CVE_CHECKED));
  }

  private static enum ToolState {

    CVE_CHECKED,

    INSTALLED
  }

}
