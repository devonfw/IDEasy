package com.devonfw.ide.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import com.devonfw.tools.ide.commandlet.Commandlet;
import com.devonfw.tools.ide.context.IdeContext;

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
}
