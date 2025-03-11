package com.devonfw.tools.ide.migration;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Abstract base implementation of {@link IdeMigration} for a single {@link #getTargetVersion() target version} (migrates from one version to the next).
 */
public abstract class IdeVersionMigration implements IdeMigration {

  private final VersionIdentifier targetVersion;

  /**
   * The constructor.
   *
   * @param targetVersion the {@link #getTargetVersion() target version}.
   */
  public IdeVersionMigration(String targetVersion) {

    this(VersionIdentifier.of(targetVersion));
  }

  /**
   * The constructor.
   *
   * @param targetVersion the {@link #getTargetVersion() target version}.
   */
  public IdeVersionMigration(VersionIdentifier targetVersion) {

    super();
    this.targetVersion = targetVersion;
  }

  @Override
  public VersionIdentifier getTargetVersion() {

    return this.targetVersion;
  }
}
