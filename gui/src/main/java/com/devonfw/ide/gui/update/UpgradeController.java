package com.devonfw.ide.gui.update;

import java.text.MessageFormat;
import javafx.application.Platform;
import javafx.concurrent.Task;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.i18n.I18nService;
import com.devonfw.ide.gui.tray.TrayNotificationService;
import com.devonfw.tools.ide.commandlet.UpgradeCommandlet;
import com.devonfw.tools.ide.tool.IdeasyCommandlet;

/**
 * Controller for tool-wide IDEasy upgrades. Keeps upgrade checks separated from project updates.
 */
public class UpgradeController {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeController.class);

  private static final String THREAD_CHECKER = "ide-gui-upgrade-checker";
  private static final String THREAD_RUNNER = "ide-gui-upgrade-runner";

  private static final String STATUS_KEY_CHECKING = "status.upgrade.checking";
  private static final String STATUS_KEY_AVAILABLE = "status.upgrade.available";
  private static final String STATUS_KEY_UP_TO_DATE = "status.upgrade.upToDate";
  private static final String STATUS_KEY_UPDATED = "status.upgrade.updated";
  private static final String STATUS_KEY_UPDATING = "status.upgrade.updating";
  private static final String STATUS_KEY_UNAVAILABLE = "status.upgrade.unavailable";
  private static final String STATUS_KEY_FAILED_PREFIX = "status.upgrade.failedPrefix";
  private static final String TOOLTIP_UPGRADE_AVAILABLE = "tooltip.upgrade.available";

  private static final String TRAY_KEY_CAPTION = "tray.upgrade.caption";
  private static final String TRAY_KEY_TEXT = "tray.upgrade.text";
  public static final String BUTTON_UPGRADE = "button.upgrade";

  private final IdeGuiStateManager manager;
  private final I18nService i18n = I18nService.getInstance();

  private StackPane upgradeIndicator;
  private Stage dialogStage;

  // Dialog FXML fields
  @FXML
  private Label statusLabel;

  @FXML
  private Button upgradeButton;


  private String currentStatusKey;
  private String currentStatusDetail;

  private String installedVersionString = "";
  private String latestVersionString = "";
  private boolean justUpgraded = false;

  public UpgradeController(IdeGuiStateManager manager) {
    this.manager = manager;
  }

  public void start(StackPane upgradeIndicator) {
    this.upgradeIndicator = upgradeIndicator;
    setStatusKey(STATUS_KEY_CHECKING);
    // indicator initially hidden until check completes
    try {
      if (this.upgradeIndicator != null) {
        this.upgradeIndicator.setVisible(false);
        Tooltip.install(this.upgradeIndicator, new Tooltip(i18n.get(TOOLTIP_UPGRADE_AVAILABLE)));
        // click handled by this controller
        this.upgradeIndicator.setOnMouseClicked(ev -> {
          ev.consume();
          Platform.runLater(this::showDialog);
        });
      }
    } catch (Throwable t) {
      LOG.debug("Failed to initialize upgrade indicator", t);
    }
    startCheck();
  }

  /**
   * Starts upgrade invoked from the dialog.
   */
  private void performUpgradeTask() {
    setStatusKey(STATUS_KEY_UPDATING);
    updateDialogStatus();

    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        performUpgrade();
        return null;
      }
    };

    task.setOnSucceeded(e -> {
      e.consume();
      // Mark as updated and prefer showing the updated message (with version)
      if (this.latestVersionString != null && !this.latestVersionString.isEmpty()) {
        this.installedVersionString = this.latestVersionString;
      }
      setStatusKey(STATUS_KEY_UPDATED);
      updateDialogStatus();
      if (upgradeButton != null) {
        upgradeButton.setDisable(true);
      }
      // after upgrade, re-check availability so the controller fetches authoritative version info
      this.justUpgraded = true;
      startCheck();
    });

    task.setOnFailed(e -> {
      e.consume();
      Throwable ex = task.getException();
      if (ex == null) {
        setStatusKey(STATUS_KEY_UNAVAILABLE);
      } else {
        setFailureDetail(ex.getMessage());
      }
      updateDialogStatus();
      if (upgradeButton != null) {
        upgradeButton.setDisable(false);
      }
    });

    startBackgroundTask(task, THREAD_RUNNER);
  }

  protected void performUpgrade() {
    IdeGuiContext ctx = new IdeGuiContext(this.manager.getStartContext(), null);
    new UpgradeCommandlet(ctx).run();
  }

  /**
   * Performs the actual upgrade availability check.
   *
   * @return true if an upgrade is available, false otherwise
   */
  protected boolean checkForUpgrade() {
    try {
      IdeGuiContext ctx = new IdeGuiContext(manager.getStartContext(), null);
      IdeasyCommandlet cmd = new IdeasyCommandlet(ctx, null);
      try {
        var installed = cmd.getInstalledVersion();
        var latest = cmd.getLatestVersion();
        this.installedVersionString = installed == null ? "" : installed.toString();
        this.latestVersionString = latest == null ? "" : latest.toString();
      } catch (Exception e) {
        LOG.debug("Failed to resolve versions", e);
        this.installedVersionString = "";
        this.latestVersionString = "";
      }
      return cmd.checkIfUpdateIsAvailable();
    } catch (Exception e) {
      LOG.debug("Upgrade check failed", e);
      return false;
    }
  }

  private void startCheck() {
    Task<Boolean> checkTask = new Task<>() {
      @Override
      protected Boolean call() {
        return checkForUpgrade();
      }
    };

    checkTask.setOnSucceeded(e -> {
      e.consume();
      boolean available = Boolean.TRUE.equals(checkTask.getValue());
      setStatusKey(available ? STATUS_KEY_AVAILABLE : STATUS_KEY_UP_TO_DATE);
      try {
        if (this.upgradeIndicator != null) {
          this.upgradeIndicator.setVisible(available);
        }
        if (this.upgradeButton != null) {
          // if dialog is open, update button state
          this.upgradeButton.setDisable(!available);
        }
      } catch (Throwable t) {
        LOG.debug("Failed to update UI on check result", t);
      }
      // Update dialog texts/labels (installed/latest versions may have changed)
      // If we just performed an upgrade and now there is no newer version available,
      // show the explicit 'updated to' confirmation message
      if (this.justUpgraded && !available) {
        // Use the localized updated message as canonical status
        if (this.latestVersionString != null && !this.latestVersionString.isEmpty()) {
          this.installedVersionString = this.latestVersionString;
        }
        setStatusKey(STATUS_KEY_UPDATED);
        updateDialogStatus();
        this.justUpgraded = false;
      } else {
        updateDialogStatus();
      }
      if (available) {
        showTrayNotification();
      }
    });

    checkTask.setOnFailed(e -> {
      e.consume();
      LOG.debug("Upgrade check failed", checkTask.getException());
      setStatusKey(STATUS_KEY_UNAVAILABLE);
      try {
        if (this.upgradeIndicator != null) {
          this.upgradeIndicator.setVisible(false);
        }
        if (this.upgradeButton != null) {
          this.upgradeButton.setDisable(true);
        }
      } catch (Throwable t) {
        LOG.debug("Failed to update UI on check failure", t);
      }
    });

    startBackgroundTask(checkTask, THREAD_CHECKER);
  }

  /**
   * Re-applies the current localized status text. For after a locale change.
   */
  public void refreshStatusText() {
    updateDialogStatus();
    try {
      if (this.upgradeIndicator != null) {
        Tooltip.install(this.upgradeIndicator, new Tooltip(i18n.get(TOOLTIP_UPGRADE_AVAILABLE)));
      }
      if (this.upgradeButton != null) {
        this.upgradeButton.setText(i18n.get(BUTTON_UPGRADE));
      }
    } catch (Throwable t) {
      LOG.debug("Failed to refresh status text", t);
    }
  }

  private void setStatusKey(String statusKey) {
    this.currentStatusKey = statusKey;
    this.currentStatusDetail = null;
  }

  private void setFailureDetail(String detail) {
    this.currentStatusKey = STATUS_KEY_FAILED_PREFIX;
    this.currentStatusDetail = detail;
  }

  private String resolveStatusText() {
    if (this.currentStatusKey == null) {
      return "";
    }
    String text = this.i18n.get(this.currentStatusKey);
    if (STATUS_KEY_AVAILABLE.equals(this.currentStatusKey) || STATUS_KEY_UP_TO_DATE.equals(this.currentStatusKey)
        || STATUS_KEY_UPDATED.equals(this.currentStatusKey)) {
      try {
        return MessageFormat.format(text, this.installedVersionString, this.latestVersionString);
      } catch (IllegalArgumentException iae) {
        LOG.debug("Failed to format status text with versions", iae);
        return text;
      }
    }
    if (this.currentStatusDetail != null) {
      return text + this.currentStatusDetail;
    }
    return text;
  }

  private void updateDialogStatus() {
    if (this.statusLabel != null) {
      this.statusLabel.setText(resolveStatusText());
    }
  }

  @FXML
  private void onUpgradeClicked() {
    performUpgradeTask();
  }

  private void showDialog() {
    try {
      // Load dialog FXML if not already loaded
      if (this.dialogStage == null) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devonfw/ide/gui/upgrade-dialog.fxml"));
        loader.setResources(i18n.getResourceBundle());
        loader.setController(this);
        Parent root = loader.load();

        this.dialogStage = new Stage();
        this.dialogStage.setTitle(i18n.get(TRAY_KEY_CAPTION));
        this.dialogStage.initModality(Modality.APPLICATION_MODAL);
        this.dialogStage.setScene(new Scene(root));
        this.dialogStage.setWidth(420);
        this.dialogStage.setHeight(160);
        this.dialogStage.setMinWidth(360);
        this.dialogStage.setMinHeight(140);
        this.dialogStage.setResizable(true);
      }

      // Update status and button state before showing/bringing to front
      updateDialogStatus();
      boolean available = STATUS_KEY_AVAILABLE.equals(this.currentStatusKey);
      if (this.upgradeButton != null) {
        this.upgradeButton.setDisable(!available);
      }

      if (this.dialogStage.isShowing()) {
        this.dialogStage.toFront();
      } else {
        this.dialogStage.show();
      }
    } catch (Throwable t) {
      LOG.debug("Failed to show upgrade dialog", t);
    }
  }

  private <T> void startBackgroundTask(Task<T> task, String threadName) {
    Thread t = new Thread(task, threadName);
    t.setDaemon(true);
    t.start();
  }


  private void showTrayNotification() {
    try {
      // attach click action that runs upgrade on FX thread
      TrayNotificationService.show(i18n.get(TRAY_KEY_CAPTION), i18n.get(TRAY_KEY_TEXT), () -> Platform.runLater(this::showDialog));
    } catch (Throwable t) {
      LOG.debug("Failed to show tray notification", t);
    }
  }
}







