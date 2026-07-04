package com.devonfw.ide.gui.modal;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Custom Alert class for IDEasy to allow interaction via the CLIs questions/modals/selections.
 */
public class IdeDialog extends Alert {

  /**
   * @param alertType the {@link AlertType} of the alert (e.g. INFORMATION, CONFIRMATION, etc).
   */
  public IdeDialog(AlertType alertType) {

    super(alertType);
    setupDefaultProperties();
  }

  /**
   * @param alertType the {@link AlertType} of the alert (e.g. INFORMATION, CONFIRMATION, etc).
   * @param message main message displayed in the dialoge
   * @param buttonTypes defines the different buttons that the alert displays.
   */
  public IdeDialog(AlertType alertType, String message, ButtonType... buttonTypes) {

    super(alertType, message, buttonTypes);
    setupDefaultProperties();
  }

  private void setupDefaultProperties() {

    setTitle("IDEasy");

    setOnShowing(event -> {
      Stage stage = (Stage) getDialogPane().getScene().getWindow();
      stage.getIcons().add(new Image("com/devonfw/ide/gui/assets/devonfw.png"));
    });
  }
}
