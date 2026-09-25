package com.devonfw.ide.gui.feature.ide.launcher;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.ide.gui.core.tab.TabView;

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

  /**
   * Creates the launcher view and loads its FXML.
   *
   * @param viewModel the view model of the launcher tab.
   * @param nlsService the localization service.
   */
  public IdeLauncherView(IdeLauncherViewModel viewModel, NlsService nlsService) {
    this.viewModel = viewModel;
    loadFxml("IdeLauncher.fxml", nlsService);
  }

  @FXML
  private void initialize() {

    this.androidStudioOpen.disableProperty().bind(this.viewModel.isIdeButtonsDisabledProperty());
    this.androidStudioOpen.setOnAction(_ -> this.viewModel.runCommandlet("android-studio"));
    this.eclipseOpen.disableProperty().bind(this.viewModel.isIdeButtonsDisabledProperty());
    this.eclipseOpen.setOnAction(_ -> this.viewModel.runCommandlet("eclipse"));
    this.intellijOpen.disableProperty().bind(this.viewModel.isIdeButtonsDisabledProperty());
    this.intellijOpen.setOnAction(_ -> this.viewModel.runCommandlet("intellij"));
    this.vsCodeOpen.disableProperty().bind(this.viewModel.isIdeButtonsDisabledProperty());
    this.vsCodeOpen.setOnAction(_ -> this.viewModel.runCommandlet("vscode"));
  }

}
