package com.devonfw.ide.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.commandlet.EnvironmentCommandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.BooleanProperty;
import com.devonfw.tools.ide.property.KeywordProperty;
import com.devonfw.tools.ide.property.Property;
import com.devonfw.tools.ide.validation.ValidationResult;

public class CommandletController {

  private static final Logger LOG = LoggerFactory.getLogger(CommandletController.class);

  private Commandlet selectedCommandlet;
  private final IdeContext context;
  private final Runnable goBackCallback;

  @FXML
  private ComboBox<String> commandletSelector;

  @FXML
  private VBox formContainer;

  @FXML
  private Button runButton;

  /// @param context
  public CommandletController(IdeContext context, Runnable goBackCallback) {
    this.context = context;
    this.goBackCallback = goBackCallback;
  }

  @FXML
  private void initialize() {
    commandletSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onCommandletSelected(newVal));
    Platform.runLater(this::populateCommandletList);
  }

  @FXML
  private void goBack() {
    this.goBackCallback.run();
  }


  private void onCommandletSelected(String name) {
    if (name == null) {
      return;
    }

    Commandlet commandlet = context.getCommandletManager().getCommandlet(name);
    this.selectedCommandlet = commandlet;

    generateFormFields(commandlet.getProperties());
  }

  private void generateFormFields(List<Property<?>> properties) {
    ObservableList<javafx.scene.Node> children = formContainer.getChildren();
    children.clear();

    for (Property<?> property : properties) {
      children.add(PropertyFormFieldFactory.createFormField(property, context));
    }
  }

  private void populateCommandletList() {
    commandletSelector.getItems().clear();
    commandletSelector.getItems().addAll(context.getCommandletManager().getCommandlets().stream()
        .map(Commandlet::getName)
        .sorted()
        .toList());
  }

  @FXML
  private void runCommandlet() {
    if (this.commandletSelector == null) {
      return;
    }

    this.selectedCommandlet.reset();

    for (javafx.scene.Node node : formContainer.getChildren()) {
      if (node instanceof javafx.scene.layout.HBox hbox && hbox.getUserData() instanceof Property<?>
          property) {
        for (javafx.scene.Node child : hbox.getChildren()) {
          if (child instanceof javafx.scene.control.TextField textField) {
            String value = textField.getText();
            if (!value.isBlank()) {
              property.assignValueAsString(value, this.context, this.selectedCommandlet);
            }
            break;
          }
        }
      }

      if (node instanceof CheckBox checkbox && checkbox.getUserData() instanceof BooleanProperty
          boolProp) {
        boolProp.setValue(checkbox.isSelected());
      }

    }

    if (!validate(this.selectedCommandlet)) {
      return;
    }

    runButton.setDisable(true);
    try {
      this.selectedCommandlet.run();
      new IdeDialog(AlertType.INFORMATION, "Commandlet executed successfully.").showAndWait();
    } catch (Exception e) {
      LOG.error("Commandlet execution failed", e);
      new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage()).showAndWait();
    } finally {
      runButton.setDisable(false);
    }

  }

  private boolean validate(Commandlet cmd) {

    KeywordProperty keyword = cmd.getFirstKeyword();
    if (keyword != null) {
      keyword.setValue(true);
    }

    ValidationResult result = cmd.validate();
    if (!result.isValid()) {
      new IdeDialog(IdeDialog.AlertType.ERROR, result.getErrorMessage()).showAndWait();
      return false;
    }

    if (cmd.isIdeHomeRequired() && this.context.getIdeHome() == null) {
      new IdeDialog(IdeDialog.AlertType.ERROR, "Not inside an IDEasy project!").showAndWait();
      return false;
    }

    if (cmd.isIdeRootRequired() && this.context.getIdeRoot() == null) {
      new IdeDialog(IdeDialog.AlertType.ERROR, "IDEasy root not found!").showAndWait();
      return false;
    }

    Path licenseAgreement = this.context.getUserHomeIde().resolve(".license.agreement");
    if (!Files.isRegularFile(licenseAgreement)) {
      if (cmd instanceof EnvironmentCommandlet) {
        return false;
      }
      new IdeDialog(IdeDialog.AlertType.ERROR, "License agreement not accepted.").showAndWait();
      return false;
    }

    return true;
  }

}
