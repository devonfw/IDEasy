package com.devonfw.tools.ide.migration;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.migration.v2025.Mig202502001;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.version.IdeVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * The entry point to {@link IdeMigration} that orchestrates all {@link IdeVersionMigration}s.
 */
public class IdeMigrator implements IdeMigration {

  /** {@link VersionIdentifier} to use as fallback if {@link IdeContext#FILE_SOFTWARE_VERSION} does not exist. */
  public static final VersionIdentifier START_VERSION = VersionIdentifier.of("2025.01.001-beta");

  private final List<IdeVersionMigration> migrations;

  /**
   * The default constructor.
   */
  public IdeMigrator() {

    // migrations must be strictly in ascending order (from oldest to newest version)
    this(List.of(new Mig202502001()));
  }

  /**
   * @param migrations the {@link List} of {@link IdeVersionMigration}s.
   */
  public IdeMigrator(List<IdeVersionMigration> migrations) {

    super();
    this.migrations = migrations;
  }

  @Override
  public VersionIdentifier getTargetVersion() {

    int size = this.migrations.size();
    int last = size - 1;
    if (last >= 0) {
      return this.migrations.get(last).getTargetVersion();
    }
    // fallback that should never happen
    return IdeVersion.getVersionIdentifier();
  }

  @Override
  public void run(IdeContext context) {

    if (context.getIdeHome() == null) {
      context.debug("Skipping migration since IDE_HOME is undefined.");
      return;
    }
    VersionIdentifier currentVersion = context.getProjectVersion();
    VersionIdentifier previousTarget = null;
    int migrationCount = 0;
    for (IdeVersionMigration migration : this.migrations) {
      VersionIdentifier targetVersion = migration.getTargetVersion();
      if (previousTarget != null) {
        if (!targetVersion.isGreater(previousTarget)) {
          throw new IllegalStateException("Invalid migration order with " + targetVersion + " after " + previousTarget);
        }
      }
      previousTarget = targetVersion;
      if ((migrationCount > 0) || currentVersion.isLess(targetVersion)) {
        Step step = context.newStep("Migrate IDEasy project from " + currentVersion + " to " + targetVersion);
        boolean success = step.run(() -> {
          migration.run(context);
        });
        if (success) {
          context.setProjectVersion(targetVersion);
          currentVersion = targetVersion;
          migrationCount++;
        } else {
          throw new IllegalStateException("Failed: " + step.getName());
        }
      } else {
        context.debug("Skipping migration {} since we are already at version {}", targetVersion, currentVersion);
      }
    }
    if (migrationCount > 0) {
      context.success("Successfully applied {} migration(s) to project {}", migrationCount, context.getProjectName());
    } else {
      context.debug("No migration to apply to project {}", context.getProjectName());
    }
  }
}
