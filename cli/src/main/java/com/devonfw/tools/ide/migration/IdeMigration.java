package com.devonfw.tools.ide.migration;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for a migration that upgrades IDEasy to the {@link #getTargetVersion() target version}.
 */
public interface IdeMigration {

  /**
   * @return the target {@link VersionIdentifier} reached when this migration was {@link #run(IdeContext) run successfully}.
   */
  VersionIdentifier getTargetVersion();

  /**
   * Performs the actual migration to the {@link #getTargetVersion() target version}.
   *
   * @param context the {@link IdeContext}.
   * @throws RuntimeException if the migration failed.
   */
  void run(IdeContext context);

}
