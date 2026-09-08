package com.devonfw.ide.gui.ui.tab.launcher;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.tab.TabView;

/**
 * View for the IDE launcher tab.
 */
public class IdeLauncherView extends TabView {

  @FXML
  private Button androidStudioOpen;

  @FXML
  private Button eclipseOpen;

  @FXML
  private Button intellijOpen;

  @FXML
  private Button vsCodeOpen;

  private final IdeLauncherViewModel viewModel;

  public IdeLauncherView(IdeLauncherViewModel viewModel, NlsService nlsService) {
    this.viewModel = viewModel;
    loadFxml("IdeLauncher.fxml", nlsService);
  }

  @FXML
  private void initialize() {

    this.androidStudioOpen.disableProperty().bindBidirectional(this.viewModel.isIdeButtonsDisabledProperty());
    this.androidStudioOpen.setOnAction(_ -> this.viewModel.runCommandlet("android-studio"));
    this.eclipseOpen.disableProperty().bindBidirectional(this.viewModel.isIdeButtonsDisabledProperty());
    this.eclipseOpen.setOnAction(_ -> this.viewModel.runCommandlet("eclipse"));
    this.intellijOpen.disableProperty().bindBidirectional(this.viewModel.isIdeButtonsDisabledProperty());
    this.intellijOpen.setOnAction(_ -> this.viewModel.runCommandlet("intellij"));
    this.vsCodeOpen.disableProperty().bindBidirectional(this.viewModel.isIdeButtonsDisabledProperty());
    this.vsCodeOpen.setOnAction(_ -> this.viewModel.runCommandlet("vscode"));
  }

}
