package com.devonfw.ide.gui.update;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.tray.TrayNotificationService;

/**
 * Handles project-specific update logic: checking whether the selected project needs an update and running the project update commandlet.
 */
public class UpdateController {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateController.class);

  private static final String THREAD_UPDATE_CHECKER = "ide-gui-update-checker";
  private static final String THREAD_UPDATE_RUNNER = "ide-gui-update-runner";

  private static final String STATUS_KEY_SELECT_PROJECT = "status.update.selectProject"; // "Select a project to check for updates"
  private static final String STATUS_KEY_CHECKING = "status.update.checking"; // "Update status: checking..."
  private static final String STATUS_KEY_AVAILABLE = "status.update.available"; // "Update available"
  private static final String STATUS_KEY_UP_TO_DATE = "status.update.upToDate"; // "Up to date"
  private static final String STATUS_KEY_UPDATING = "status.update.updating"; // "Updating..."
  private static final String STATUS_KEY_COMPLETED = "status.update.completed"; // "Update completed"
  private static final String STATUS_KEY_UNAVAILABLE = "status.update.unavailable"; // "Update status unavailable"
  private static final String STATUS_KEY_FAILED_PREFIX = "status.update.failedPrefix"; // "Update failed: " (prefix, detail text appended)

  private static final String DIALOG_KEY_COMPLETED = "dialog.update.completed"; // "Update completed"

  private static final String TRAY_KEY_CAPTION = "tray.update.caption"; // "IDEasy updates available"
  private static final String TRAY_KEY_TEXT = "tray.update.text"; // "Click the IDEasy dashboard to apply updates."
  private static final String TOOLTIP_KEY_AVAILABLE = "tooltip.update.available"; // "Update available - click the icon to view details"
  private static final String BUTTON_KEY_UPDATE = "button.update"; // "Update"

  private static final double POST_UPDATE_RECHECK_DELAY_MILLIS = 500d;

  private final GuiStateManager manager;
  private final NlsService nlsService;
  private final UpdateService updateService;

  private StackPane updateIndicator;
  private Stage dialogStage;

  @FXML
  private Label statusLabel;

  @FXML
  private Button executeButton;

  private IdeGuiContext currentContext;

  /** Whether the last known status is "update available" (drives the dialog's button state on open). */
  private boolean updateAvailable;

  /** The last rendered status text, kept so {@link #showDialog()} can paint it once the label becomes available (it is null until the dialog first loads). */
  private String currentStatusText = "";

  /**
   * The constructor.
   *
   * @param manager the {@link GuiStateManager} to use.
   * @param nlsService the {@link NlsService} to use.
   */
  public UpdateController(GuiStateManager manager, NlsService nlsService) {
    this(manager, nlsService, new UpdateService());
  }

  /**
   * The constructor with an injectable {@link UpdateService}.
   *
   * @param manager the {@link GuiStateManager} to use.
   * @param nlsService the {@link NlsService} to use.
   * @param updateService the {@link UpdateService} encapsulating the update business logic.
   */
  public UpdateController(GuiStateManager manager, NlsService nlsService, UpdateService updateService) {
    this.manager = manager;
    this.nlsService = nlsService;
    this.updateService = updateService;
  }

  /**
   * Start the update controller: wire the indicator and initialize the status based on the current project context.
   *
   * @param updateIndicator the indicator shown next to the project combo box
   */
  public void start(StackPane updateIndicator) {
    this.updateIndicator = updateIndicator;
    try {
      if (this.updateIndicator != null) {
        this.updateIndicator.setVisible(false);
        Tooltip.install(this.updateIndicator, new Tooltip(nlsService.get(TOOLTIP_KEY_AVAILABLE)));
        this.updateIndicator.setOnMouseClicked(ev -> {
          ev.consume();
          Platform.runLater(this::showDialog);
        });
      }
    } catch (Throwable t) {
      LOG.debug("Failed to initialize update indicator", t);
    }
    onContextChanged(this.manager.getCurrentContext());
  }

  /**
   * Called when the user clicks the update button in the shared dialog.
   */
  @FXML
  private void onExecuteClicked() {
    IdeGuiContext context = this.currentContext;
    if (context == null) {
      showStatus(STATUS_KEY_SELECT_PROJECT);
      setUpdateButtonDisabled(true);
      return;
    }

    setUpdatingState();

    TasksHelper.run(() -> {
      this.updateService.runUpdate(context);
      return null;
    }, ignored -> {
      showUpdateCompleted();

      try {
        // show a notification dialog but do not allow failures here to prevent
        // breaking the update flow if the dialog fails to show
        new IdeDialog(IdeDialog.AlertType.INFORMATION, this.nlsService.get(DIALOG_KEY_COMPLETED)).show();
      } catch (Throwable t) {
        LOG.debug("Failed to show completion dialog", t);
      }
      // Delay the post-update re-check slightly so the UI shows "Update completed"
      // for a brief moment before switching to the final status.
      scheduleDelayedRecheck();
    }, throwable -> {
      if (throwable == null) {
        throwable = new RuntimeException("Update failed");
      }

      String detail = throwable.getMessage();
      if ((detail == null) || detail.isBlank()) {
        detail = throwable.getClass().getSimpleName();
      }
      showUpdateFailed(detail);

      try {
        new IdeDialog(IdeDialog.AlertType.ERROR, this.nlsService.get(STATUS_KEY_FAILED_PREFIX) + detail).show();
      } catch (Throwable ex) {
        LOG.debug("Failed to show failure dialog", ex);
      }
    }, THREAD_UPDATE_RUNNER);
  }

  /**
   * Called by the GUI when the selected project/workspace context changes.
   *
   * @param currentContext the new current project context, or {@code null} if no project is selected yet.
   */
  public void onContextChanged(IdeGuiContext currentContext) {

    this.currentContext = currentContext;
    if (currentContext == null) {
      showStatus(STATUS_KEY_SELECT_PROJECT);
      setUpdateButtonDisabled(true);
      if (this.updateIndicator != null) {
        this.updateIndicator.setVisible(false);
      }
      return;
    }

    showStatus(STATUS_KEY_CHECKING);
    setUpdateButtonDisabled(true);
    // perform an initial check automatically
    startUpdateCheck(currentContext);
  }

  /**
   * Perform the project update check asynchronously and notify via the JavaFX thread.
   */
  private void startUpdateCheck(IdeGuiContext context) {

    TasksHelper.run(() -> this.updateService.isUpdateAvailable(context), available -> {
      if (this.currentContext == context) {
        applyCheckResult(available);
      }
    }, throwable -> {
      LOG.warn("Update check failed", throwable);
      showUpdateFailed(nlsService.get(STATUS_KEY_UNAVAILABLE));
    }, THREAD_UPDATE_CHECKER);
  }

  private void applyCheckResult(boolean updateAvailable) {
    showStatus(updateAvailable ? STATUS_KEY_AVAILABLE : STATUS_KEY_UP_TO_DATE);
    setUpdateButtonDisabled(!updateAvailable);
    if (this.updateIndicator != null) {
      this.updateIndicator.setVisible(updateAvailable);
    }

    if (updateAvailable) {
      showTrayNotification();
    }
  }

  private void setUpdatingState() {
    setUpdateButtonDisabled(true);
    showStatus(STATUS_KEY_UPDATING);
  }

  private void showUpdateCompleted() {
    showStatus(STATUS_KEY_COMPLETED);
    setUpdateButtonDisabled(true);
  }

  private void showUpdateFailed(String detail) {
    showStatusFailed(detail);
    setUpdateButtonDisabled(false);
  }

  /**
   * Shows a plain localized status and records whether it represents "update available" (for the dialog's button state).
   */
  private void showStatus(String key) {
    this.updateAvailable = STATUS_KEY_AVAILABLE.equals(key);
    setLabelText(this.nlsService.get(key));
  }

  /**
   * Shows the failed-status prefix with an appended (not localized) detail, e.g. an exception message.
   */
  private void showStatusFailed(String detail) {
    this.updateAvailable = false;
    String text = this.nlsService.get(STATUS_KEY_FAILED_PREFIX);
    if (detail != null) {
      text = text + detail;
    }
    setLabelText(text);
  }

  private void setLabelText(String text) {
    this.currentStatusText = text;
    if (this.statusLabel != null) {
      this.statusLabel.setText(text);
    }
  }

  private void setUpdateButtonDisabled(boolean disabled) {
    if (this.executeButton != null) {
      this.executeButton.setDisable(disabled);
    }
  }

  private void showTrayNotification() {
    try {
      TrayNotificationService.show(this.nlsService.get(TRAY_KEY_CAPTION), this.nlsService.get(TRAY_KEY_TEXT), () -> Platform.runLater(this::showDialog));
    } catch (Throwable t) {
      LOG.debug("Failed to show tray notification", t);
    }
  }

  private void showDialog() {
    try {
      if (this.dialogStage == null) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devonfw/ide/gui/upgrade-update-dialog.fxml"));
        loader.setResources(nlsService.getResourceBundle());
        loader.setController(this);
        Parent root = loader.load();

        this.dialogStage = new Stage();
        this.dialogStage.setTitle(nlsService.get(TRAY_KEY_CAPTION));
        this.dialogStage.initModality(Modality.APPLICATION_MODAL);
        this.dialogStage.setScene(new Scene(root));
        this.dialogStage.setWidth(420);
        this.dialogStage.setHeight(160);
        this.dialogStage.setMinWidth(360);
        this.dialogStage.setMinHeight(140);
        this.dialogStage.setResizable(true);
      }

      // repaint now that the label/button are bound: they may have missed status updates issued before the dialog was first loaded
      setLabelText(this.currentStatusText);
      if (this.executeButton != null) {
        this.executeButton.setText(nlsService.get(BUTTON_KEY_UPDATE));
        this.executeButton.setDisable(!this.updateAvailable);
      }

      if (this.dialogStage.isShowing()) {
        this.dialogStage.toFront();
      } else {
        this.dialogStage.show();
      }
    } catch (Throwable t) {
      LOG.debug("Failed to show update dialog", t);
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
