package com.devonfw.ide.gui.update;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.tools.ide.commandlet.UpdateCommandlet;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.migration.IdeMigrator;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Encapsulates the project-update business logic (checking for and running settings/migration updates for the selected project), independent of any UI
 * framework.
 */
public class UpdateService {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateService.class);

  // Mock override for testing/dev runs: if set (non-null), isUpdateAvailable will return this value instead of performing real checks.
  private static Boolean mockUpdateAvailable = null;

  /**
   * Sets a mock update availability for testing/dev runs. If set to a non-null value, {@link #isUpdateAvailable(IdeGuiContext)} will return that value
   * instead of performing real update checks. Pass null to disable the mock and use real checks.
   *
   * @param available true to indicate update is available, false for no update, null to disable mock
   */
  public static void setMockUpdateAvailable(Boolean available) {
    mockUpdateAvailable = available;
  }

  /**
   * @param context the current project context, may be {@code null} if no project is selected.
   * @return {@code true} if any project update (settings update or migration) is available.
   */
  public boolean isUpdateAvailable(IdeGuiContext context) {

    if (mockUpdateAvailable != null) {
      return mockUpdateAvailable;
    }

    if (context == null) {
      return false;
    }

    boolean updateAvailable = false;

    try {
      updateAvailable = checkSettingsUpdate(context);
    } catch (Exception e) {
      LOG.debug("Failed to check settings repository update", e);
    }

    try {
      updateAvailable = updateAvailable || checkProjectMigration(context);
    } catch (Exception e) {
      LOG.debug("Failed to check project migration status", e);
    }

    return updateAvailable;
  }

  /**
   * Runs the project update commandlet against the given context.
   *
   * @param context the current project context.
   */
  public void runUpdate(IdeGuiContext context) {

    context.getCommandletManager().getCommandlet(UpdateCommandlet.class).run();
  }

  private boolean checkSettingsUpdate(IdeGuiContext context) {

    Path settingsRepository = context.getSettingsGitRepository();
    if (settingsRepository == null) {
      return false;
    }

    GitContext gitContext = context.getGitContext();
    return gitContext.isRepositoryUpdateAvailable(settingsRepository, context.getSettingsCommitIdPath())
        || (gitContext.fetchIfNeeded(settingsRepository)
        && gitContext.isRepositoryUpdateAvailable(settingsRepository, context.getSettingsCommitIdPath()));
  }

  private boolean checkProjectMigration(IdeGuiContext context) {

    IdeMigrator migrator = new IdeMigrator();
    VersionIdentifier projectVersion = context.getProjectVersion();
    VersionIdentifier targetVersion = migrator.getTargetVersion();
    return projectVersion.isLess(targetVersion);
  }
}
