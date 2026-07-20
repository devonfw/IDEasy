package com.devonfw.ide.gui.update;

import java.text.MessageFormat;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.ide.gui.tray.TrayNotificationService;

/**
 * Controller for tool-wide IDEasy upgrades. Keeps upgrade checks separated from project updates.
 */
public class UpgradeController {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeController.class);

  private static final String THREAD_CHECKER = "ide-gui-upgrade-checker";
  private static final String THREAD_RUNNER = "ide-gui-upgrade-runner";

  private static final String STATUS_KEY_CHECKING = "status.upgrade.checking"; // "Upgrade status: checking..."
  private static final String STATUS_KEY_AVAILABLE = "status.upgrade.available"; // "Version {0} needs to be updated to {1}."
  private static final String STATUS_KEY_UP_TO_DATE = "status.upgrade.upToDate"; // "Version {0} is the latest version."
  private static final String STATUS_KEY_UPDATED = "status.upgrade.updated"; // "IDEasy updated to latest version: {0}"
  private static final String STATUS_KEY_UPDATING = "status.upgrade.updating"; // "Upgrading IDEasy..."
  private static final String STATUS_KEY_UNAVAILABLE = "status.upgrade.unavailable"; // "Upgrade status unavailable"
  private static final String STATUS_KEY_FAILED_PREFIX = "status.upgrade.failedPrefix"; // "Upgrade failed: " (prefix, detail text appended)
  private static final String TOOLTIP_UPGRADE_AVAILABLE = "tooltip.upgrade.available"; // "Upgrade available - click the icon to view details"

  private static final String TRAY_KEY_CAPTION = "tray.upgrade.caption"; // "IDEasy upgrade available"
  private static final String TRAY_KEY_TEXT = "tray.upgrade.text"; // "Click to start IDEasy upgrade."

  private final NlsService nlsService;
  private final UpgradeService upgradeService;

  private StackPane upgradeIndicator;
  private Stage dialogStage;

  // Dialog FXML fields
  @FXML
  private Label statusLabel;

  @FXML
  private Button executeButton;

  /** Whether the last known status is "upgrade available" (drives the dialog's button state on open). */
  private boolean upgradeAvailable;

  /** The last rendered status text, kept so {@link #showDialog()} can paint it once the label becomes available (it is null until the dialog first loads). */
  private String currentStatusText = "";

  private String installedVersionString = "";
  private String latestVersionString = "";
  private boolean justUpgraded = false;

  /**
   * The constructor.
   *
   * @param manager the {@link IdeGuiStateManager} to use.
   * @param nlsService the {@link NlsService} to use.
   */
  public UpgradeController(IdeGuiStateManager manager, NlsService nlsService) {
    this(nlsService, new UpgradeService(manager));
  }

  /**
   * The constructor with an injectable {@link UpgradeService}.
   *
   * @param nlsService the {@link NlsService} to use.
   * @param upgradeService the {@link UpgradeService} encapsulating the upgrade business logic.
   */
  public UpgradeController(NlsService nlsService, UpgradeService upgradeService) {
    this.nlsService = nlsService;
    this.upgradeService = upgradeService;
  }

  public void start(StackPane upgradeIndicator) {
    this.upgradeIndicator = upgradeIndicator;
    showStatus(STATUS_KEY_CHECKING);
    // indicator initially hidden until check completes
    try {
      if (this.upgradeIndicator != null) {
        this.upgradeIndicator.setVisible(false);
        Tooltip.install(this.upgradeIndicator, new Tooltip(nlsService.get(TOOLTIP_UPGRADE_AVAILABLE)));
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
    showStatus(STATUS_KEY_UPDATING);

    TasksHelper.run(() -> {
      this.upgradeService.runUpgrade();
      return null;
    }, ignored -> {
      // Mark as updated and prefer showing the updated message (with version)
      if (this.latestVersionString != null && !this.latestVersionString.isEmpty()) {
        this.installedVersionString = this.latestVersionString;
      }
      showStatus(STATUS_KEY_UPDATED);
      if (executeButton != null) {
        executeButton.setDisable(true);
      }
      // after upgrade, re-check availability so the controller fetches authoritative version info
      this.justUpgraded = true;
      startCheck();
    }, throwable -> {
      if (throwable == null) {
        showStatus(STATUS_KEY_UNAVAILABLE);
      } else {
        showStatusFailed(throwable.getMessage());
      }
      if (executeButton != null) {
        executeButton.setDisable(false);
      }
    }, THREAD_RUNNER);
  }

  private void startCheck() {

    TasksHelper.run(this.upgradeService::checkForUpgrade, result -> {
      boolean available = Boolean.TRUE.equals(result);
      this.installedVersionString = this.upgradeService.getInstalledVersion();
      this.latestVersionString = this.upgradeService.getLatestVersion();

      showStatus(available ? STATUS_KEY_AVAILABLE : STATUS_KEY_UP_TO_DATE);
      try {
        if (this.upgradeIndicator != null) {
          this.upgradeIndicator.setVisible(available);
        }
        if (this.executeButton != null) {
          // if dialog is open, update button state
          this.executeButton.setDisable(!available);
        }
      } catch (Throwable t) {
        LOG.debug("Failed to update UI on check result", t);
      }
      // If we just performed an upgrade and now there is no newer version available,
      // show the explicit 'updated to' confirmation message
      if (this.justUpgraded && !available) {
        // Use the localized updated message as canonical status
        if (this.latestVersionString != null && !this.latestVersionString.isEmpty()) {
          this.installedVersionString = this.latestVersionString;
        }
        showStatus(STATUS_KEY_UPDATED);
        this.justUpgraded = false;
      }
      if (available) {
        showTrayNotification();
      }
    }, throwable -> {
      LOG.debug("Upgrade check failed", throwable);
      showStatus(STATUS_KEY_UNAVAILABLE);
      try {
        if (this.upgradeIndicator != null) {
          this.upgradeIndicator.setVisible(false);
        }
        if (this.executeButton != null) {
          this.executeButton.setDisable(true);
        }
      } catch (Throwable t) {
        LOG.debug("Failed to update UI on check failure", t);
      }
    }, THREAD_CHECKER);
  }

  /**
   * Shows a plain localized status (with version substitution for available/up-to-date/updated) and records whether it represents "upgrade available" (for the
   * dialog's button state).
   */
  private void showStatus(String key) {
    this.upgradeAvailable = STATUS_KEY_AVAILABLE.equals(key);
    setLabelText(formatStatusText(key));
  }

  /**
   * Shows the failed-status prefix with an appended (not localized) detail, e.g. an exception message.
   */
  private void showStatusFailed(String detail) {
    this.upgradeAvailable = false;
    String text = this.nlsService.get(STATUS_KEY_FAILED_PREFIX);
    if (detail != null) {
      text = text + detail;
    }
    setLabelText(text);
  }

  private String formatStatusText(String key) {
    String text = this.nlsService.get(key);
    if (STATUS_KEY_AVAILABLE.equals(key) || STATUS_KEY_UP_TO_DATE.equals(key) || STATUS_KEY_UPDATED.equals(key)) {
      try {
        return MessageFormat.format(text, this.installedVersionString, this.latestVersionString);
      } catch (IllegalArgumentException iae) {
        LOG.debug("Failed to format status text with versions", iae);
        return text;
      }
    }
    return text;
  }

  private void setLabelText(String text) {
    this.currentStatusText = text;
    if (this.statusLabel != null) {
      this.statusLabel.setText(text);
    }
  }

  @FXML
  private void onExecuteClicked() {
    performUpgradeTask();
  }

  private void showDialog() {
    try {
      // Load dialog FXML if not already loaded
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
        this.executeButton.setDisable(!this.upgradeAvailable);
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

  private void showTrayNotification() {
    try {
      // attach click action that runs upgrade on FX thread
      TrayNotificationService.show(nlsService.get(TRAY_KEY_CAPTION), nlsService.get(TRAY_KEY_TEXT), () -> Platform.runLater(this::showDialog));
    } catch (Throwable t) {
      LOG.debug("Failed to show tray notification", t);
    }
  }
}
