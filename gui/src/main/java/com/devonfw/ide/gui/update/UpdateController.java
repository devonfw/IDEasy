package com.devonfw.ide.gui.update;

import java.nio.file.Path;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.i18n.I18nService;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.tray.TrayNotificationService;
import com.devonfw.tools.ide.commandlet.UpdateCommandlet;
import com.devonfw.tools.ide.git.GitContext;
import com.devonfw.tools.ide.migration.IdeMigrator;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Handles project-specific update logic: checking whether the selected project needs an update and running the project update commandlet.
 */
public class UpdateController {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateController.class);

  private static final String THREAD_UPDATE_CHECKER = "ide-gui-update-checker";
  private static final String THREAD_UPDATE_RUNNER = "ide-gui-update-runner";

  // Mock override for testing: if set (non-null), checkForUpdates will return this value instead of performing real checks
  private static Boolean mockUpdateAvailable = null;

  private static final String STATUS_KEY_SELECT_PROJECT = "status.update.selectProject";
  private static final String STATUS_KEY_CHECKING = "status.update.checking";
  private static final String STATUS_KEY_AVAILABLE = "status.update.available";
  private static final String STATUS_KEY_UP_TO_DATE = "status.update.upToDate";
  private static final String STATUS_KEY_UPDATING = "status.update.updating";
  private static final String STATUS_KEY_COMPLETED = "status.update.completed";
  private static final String STATUS_KEY_UNAVAILABLE = "status.update.unavailable";
  private static final String STATUS_KEY_FAILED_PREFIX = "status.update.failedPrefix";

  private static final String DIALOG_KEY_COMPLETED = "dialog.update.completed";

  private static final String TRAY_KEY_CAPTION = "tray.update.caption";
  private static final String TRAY_KEY_TEXT = "tray.update.text";


  private static final double POST_UPDATE_RECHECK_DELAY_MILLIS = 500d;

  private final IdeGuiStateManager manager;
  private final I18nService i18n = I18nService.getInstance();

  private Label updateStatusLabel;
  private Button updateButton;

  private IdeGuiContext currentContext;

  private String currentStatusKey;
  private String currentStatusDetail;

  public UpdateController(IdeGuiStateManager manager) {
    this.manager = manager;
  }

  /**
   * Sets a mock update availability for testing. If set to a non-null value, {@link #checkForUpdates(IdeGuiContext)} will return that value instead of
   * performing real update checks. Pass null to disable the mock and use real checks.
   *
   * @param available true to indicate update is available, false for no update, null to disable mock
   */
  public static void setMockUpdateAvailable(Boolean available) {
    mockUpdateAvailable = available;
  }


  /**
   * Start the update controller: wire UI state and initialize the status based on the current project context.
   *
   * @param updateStatusLabel label to show status messages
   * @param updateButton button that will trigger update action
   */
  public void start(Label updateStatusLabel, Button updateButton) {
    this.updateStatusLabel = updateStatusLabel;
    this.updateButton = updateButton;
    onContextChanged(this.manager.getCurrentContext());
  }

  /**
   * Called when the user clicks the update button in the UI.
   */
  public void onUpdateClicked() {
    IdeGuiContext context = this.currentContext;
    if (context == null) {
      setStatusKey(STATUS_KEY_SELECT_PROJECT);
      setUpdateButtonDisabled(true);
      return;
    }

    setUpdatingState();

    Task<Void> updateTask = new Task<>() {

      @Override
      protected Void call() {
        performProjectUpdate(context);
        return null;
      }
    };

    updateTask.setOnSucceeded(ignored -> {
      showUpdateCompleted();

      try {
        // show a notification dialog but do not allow failures here to prevent
        // breaking the update flow in environments where dialogs are not
        // supported (headless CI).
        new IdeDialog(IdeDialog.AlertType.INFORMATION, this.i18n.get(DIALOG_KEY_COMPLETED)).show();
      } catch (Throwable t) {
        LOG.debug("Failed to show completion dialog", t);
      }

      // Delay the post-update re-check slightly so the UI shows "Update completed"
      // for a brief moment before switching to the final status. This improves UX
      // and makes transient states observable in tests.
      scheduleDelayedRecheck();
    });

    updateTask.setOnFailed(ignored -> {
      Throwable throwable = updateTask.getException();
      if (throwable == null) {
        throwable = new RuntimeException("Update failed");
      }

      String detail = throwable.getMessage();
      if ((detail == null) || detail.isBlank()) {
        detail = throwable.getClass().getSimpleName();
      }
      showUpdateFailed(detail);

      try {
        new IdeDialog(IdeDialog.AlertType.ERROR, resolveStatusText()).show();
      } catch (Throwable ex) {
        LOG.debug("Failed to show failure dialog", ex);
      }
    });

    startBackgroundTask(updateTask, THREAD_UPDATE_RUNNER);
  }

  /**
   * Called by the GUI when the selected project/workspace context changes.
   *
   * @param currentContext the new current project context, or {@code null} if no project is selected yet.
   */
  public void onContextChanged(IdeGuiContext currentContext) {

    this.currentContext = currentContext;
    if (currentContext == null) {
      setStatusKey(STATUS_KEY_SELECT_PROJECT);
      setUpdateButtonDisabled(true);
      return;
    }

    setStatusKey(STATUS_KEY_CHECKING);
    setUpdateButtonDisabled(true);
    // perform an initial check automatically
    startUpdateCheck(currentContext);
  }

  /**
   * Re-applies the current localized status text. Useful after a locale change.
   */
  public void refreshStatusText() {

    setStatusText(resolveStatusText());
  }

  /**
   * Performs the actual project update work. Tests can override this to avoid executing the real commandlet.
   *
   * @param context the current project context.
   */
  protected void performProjectUpdate(IdeGuiContext context) {

    context.getCommandletManager().getCommandlet(UpdateCommandlet.class).run();
  }

  /**
   * Perform the project update check asynchronously and notify via the JavaFX thread. The callback receives {@code true} if a project update (settings update
   * or migration) is available.
   */
  private void startUpdateCheck(IdeGuiContext context) {
    Task<Boolean> checkTask = new Task<>() {

      @Override
      protected Boolean call() {
        // Delegate to an overridable method so tests can provide deterministic
        // behavior without running external commandlets or network operations.
        return checkForUpdates(context);
      }
    };

    checkTask.setOnSucceeded(ignored -> {
      boolean updateAvailable = Boolean.TRUE.equals(checkTask.getValue());
      if (this.currentContext == context) {
        applyCheckResult(updateAvailable);
      }
    });

    checkTask.setOnFailed(ignored -> {
      LOG.warn("Update check failed", checkTask.getException());
      showUpdateFailed(i18n.get(STATUS_KEY_UNAVAILABLE));
    });

    startBackgroundTask(checkTask, THREAD_UPDATE_CHECKER);
  }

  /**
   * Default implementation that performs the real checks. Tests can override this to make update checks deterministic.
   * <p>
   * If {@link #setMockUpdateAvailable(Boolean)} was called with a non-null value, that value is returned instead of performing real checks.
   *
   * @param context the current project context.
   * @return true if any project update is available, false otherwise
   */
  protected boolean checkForUpdates(IdeGuiContext context) {

    // If mock is set (for testing)
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

  private void applyCheckResult(boolean updateAvailable) {
    if (updateAvailable) {
      setStatusKey(STATUS_KEY_AVAILABLE);
    } else {
      setStatusKey(STATUS_KEY_UP_TO_DATE);
    }

    setUpdateButtonDisabled(!updateAvailable);

    if (updateAvailable) {
      showTrayNotification();
    }
  }

  private boolean checkSettingsUpdate(IdeGuiContext currentContext) {
    Path settingsRepository = currentContext.getSettingsGitRepository();
    if (settingsRepository == null) {
      return false;
    }

    GitContext gitContext = currentContext.getGitContext();
    return gitContext.isRepositoryUpdateAvailable(settingsRepository, currentContext.getSettingsCommitIdPath())
        || (gitContext.fetchIfNeeded(settingsRepository)
        && gitContext.isRepositoryUpdateAvailable(settingsRepository, currentContext.getSettingsCommitIdPath()));
  }

  private boolean checkProjectMigration(IdeGuiContext currentContext) {
    IdeMigrator migrator = new IdeMigrator();
    VersionIdentifier projectVersion = currentContext.getProjectVersion();
    VersionIdentifier targetVersion = migrator.getTargetVersion();
    return projectVersion.isLess(targetVersion);
  }

  private void setUpdatingState() {
    setUpdateButtonDisabled(true);
    setStatusKey(STATUS_KEY_UPDATING);
  }

  private void showUpdateCompleted() {
    setStatusKey(STATUS_KEY_COMPLETED);
    setUpdateButtonDisabled(true);
  }

  private void showUpdateFailed(String detail) {
    setFailureDetail(detail);
    setUpdateButtonDisabled(false);
  }

  private void setStatusKey(String statusKey) {

    this.currentStatusKey = statusKey;
    this.currentStatusDetail = null;
    refreshStatusText();
  }

  private void setFailureDetail(String detail) {
    this.currentStatusKey = STATUS_KEY_FAILED_PREFIX;
    this.currentStatusDetail = detail;
    refreshStatusText();
  }

  private String resolveStatusText() {

    if (this.currentStatusKey == null) {
      return "";
    }
    String text = this.i18n.get(this.currentStatusKey);
    if (this.currentStatusDetail != null) {
      return text + this.currentStatusDetail;
    }
    return text;
  }

  private void setStatusText(String text) {
    if (this.updateStatusLabel != null) {
      this.updateStatusLabel.setText(text);
    }
  }

  private void setUpdateButtonDisabled(boolean disabled) {
    if (this.updateButton != null) {
      this.updateButton.setDisable(disabled);
    }
  }

  private <T> void startBackgroundTask(Task<T> task, String threadName) {
    Thread thread = new Thread(task, threadName);
    thread.setDaemon(true);
    thread.start();
  }

  private void showTrayNotification() {
    try {
      TrayNotificationService.show(this.i18n.get(TRAY_KEY_CAPTION), this.i18n.get(TRAY_KEY_TEXT), null);
    } catch (Throwable t) {
      LOG.debug("Failed to show tray notification", t);
    }
  }

  private void scheduleDelayedRecheck() {
    try {
      PauseTransition pause = new PauseTransition(Duration.millis(POST_UPDATE_RECHECK_DELAY_MILLIS));
      pause.setOnFinished(event -> {
        LOG.debug("Pause finished, scheduling post-update recheck: {}", event);
        try {
          startUpdateCheck(this.currentContext);
        } catch (Throwable t) {
          LOG.debug("Failed to start delayed post-update update check", t);
        }
      });
      pause.play();
    } catch (Throwable t) {
      LOG.debug("Failed to schedule delayed re-check", t);

      // fallback to immediate check
      try {
        onContextChanged(this.currentContext);
      } catch (Throwable ex) {
        LOG.debug("Fallback startUpdateCheck failed", ex);
      }
    }
  }
}
