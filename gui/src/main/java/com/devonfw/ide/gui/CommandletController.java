package com.devonfw.ide.gui;

import java.util.List;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;

public class CommandletController {

  private Commandlet selectedCommandlet;
  private final IdeContext context;

  @FXML
  private ComboBox<String> commandletSelector;

  @FXML
  private VBox formContainer;

  @FXML
  private Button runButton;

  /// @param context
  public CommandletController(IdeContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    commandletSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onCommandletSelected(newVal));
    Platform.runLater(this::populateCommandletList);
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
        .toList());
  }

  @FXML
  private void runCommandlet() {
    if (this.commandletSelector == null) {
      return;
    }

    for (javafx.scene.Node node : formContainer.getChildren()) {
      if (node instanceof javafx.scene.layout.HBox hbox && hbox.getUserData() instanceof Property<?>
          property) {
        for (javafx.scene.Node child : hbox.getChildren()) {
          if (child instanceof javafx.scene.control.TextField textField) {
            property.setValueAsString(textField.getText(), context);
            break;
          }
        }
      }
    }

    this.selectedCommandlet.run();
  }
}
