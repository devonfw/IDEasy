package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Enum} with available modes for {@link UpgradeCommandlet upgrade}.
 */
public enum UpgradeMode {

  /** Mode to get latest stable release version. */
  STABLE(VersionIdentifier.LATEST),

  /** Mode to get latest stable release version even if unstable. */
  UNSTABLE(VersionIdentifier.LATEST_UNSTABLE),

  /** Mode to get latest SNAPSHOT release version. */
  SNAPSHOT(VersionIdentifier.of("*!-SNAPSHOT"));

  private final VersionIdentifier version;

  private UpgradeMode(VersionIdentifier version) {
    this.version = version;
  }

  /**
   * @return the {@link VersionIdentifier version pattern} for this {@link UpgradeMode}.
   */
  public VersionIdentifier getVersion() {
    return this.version;
  }
}
