package com.devonfw.ide.gui;

import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.property.Property;

public class CommandletController {

  private Commandlet selectedCommandlet;
  private final IdeContext context;

  @FXML
  private ComboBox<String> commandletSelector;
  @FXML
  private TextField parameterField;
  @FXML
  private Button runButton;

  public CommandletController(IdeContext context) {
    this.context = context;
  }

  @FXML
  private void initialize() {
    Platform.runLater(this::populateCommandletList);
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
    String name = commandletSelector.getValue();
    if (name == null) {
      return;
    }

    Commandlet commandlet = context.getCommandletManager().getCommandlet(name);

    String input = parameterField.getText();
    String[] tokens = input.split("\\s+");

    List<Property<?>> values = commandlet.getValues();

    for (int i = 0; i < tokens.length && (i + 1) < values.size(); i++) {
      values.get(i + 1).setValueAsString(tokens[i], context);
    }

    commandlet.run();
  }
}
