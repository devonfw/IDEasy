package com.devonfw.ide.gui.ui.controls.mainwindow;


import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import com.devonfw.ide.gui.helper.FxHelper;
import com.devonfw.ide.gui.service.NlsService;

public class NavigationPanelView extends VBox {

  @FXML
  private ComboBox<String> projects;

  @FXML
  private ComboBox<String> workspaces;

  @FXML
  private ComboBox<String> languages;

  private final NlsService nlsService;

  private final NavigationPanelViewModel viewModel;

  /**
   * Creates the navigation panel and loads its FXML, resolving the default-locale bundle for the {@code %key} text.
   */
  public NavigationPanelView(NavigationPanelViewModel viewModel, NlsService nlsService) {
    super();

    this.nlsService = nlsService;
    this.viewModel = viewModel;

    FxHelper.loadFxml("NavigationPanel.fxml", nlsService, this);
  }

  @FXML
  private void initialize() {
    this.projects.setItems(viewModel.getProjectsViewModel().getItems());
    this.projects.valueProperty().bindBidirectional(viewModel.getProjectsViewModel().selectedItemProperty());
    this.projects.getSelectionModel().selectedItemProperty().addListener((_, _, new_value) -> {
      if (new_value != null) {
        viewModel.populateWorkspaceComboBox();
      }
    });

    this.workspaces.setItems(viewModel.getWorkspacesViewModel().getItems());
    this.workspaces.valueProperty().bindBidirectional(viewModel.getWorkspacesViewModel().selectedItemProperty());
    this.workspaces.disableProperty().bind(viewModel.getProjectsViewModel().selectedItemProperty().isNull());
    this.workspaces.getSelectionModel().selectedItemProperty().addListener((_, _, new_value) -> {
      if (new_value != null) {
        viewModel.updateContext();
      }
    });

    this.languages.setItems(viewModel.getLanguagesViewModel().getItems());
    this.languages.valueProperty().bindBidirectional(viewModel.getLanguagesViewModel().selectedItemProperty());
  }
}
