package com.devonfw.ide.gui.ui.controls.mainwindow;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import com.devonfw.ide.gui.ui.mainwindow.MainWindowViewModel;

/**
 * Controller for the main-window navigation panel (the left project/workspace/language sidebar).
 */
public class NavigationPanelControl extends VBox {

  @FXML
  private ComboBox<String> projects;

  @FXML
  private ComboBox<String> workspaces;

  @FXML
  private ComboBox<String> languages;

  /**
   * Creates the navigation panel and loads its FXML, resolving the default-locale bundle for the {@code %key} text.
   */
  public NavigationPanelControl() {
    super();

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("NavigationPanelControl.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    // This control self-loads its FXML in a no-arg constructor, so it has no NlsService reference here. The FXML uses %key text, so it needs a
    // ResourceBundle to resolve them. Resolve the same default-locale bundle the app loads at startup (see NlsService). This is to be removed when
    // language selection is reworked.
    loader.setResources(ResourceBundle.getBundle("nls.messages", Locale.getDefault()));

    try {
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void initialize() {

  }

  /**
   * Binds the three combo boxes to the selections held by the main window's view model.
   *
   * @param viewModel the MainWindowViewModel
   */
  public void bind(MainWindowViewModel viewModel) {

    this.projects.setItems(viewModel.getProjectsViewModel().getItems());
    this.projects.valueProperty().bindBidirectional(viewModel.getProjectsViewModel().selectedItemProperty());

    this.workspaces.setItems(viewModel.getWorkspacesViewModel().getItems());
    this.workspaces.valueProperty().bindBidirectional(viewModel.getWorkspacesViewModel().selectedItemProperty());
    this.workspaces.disableProperty().bind(viewModel.getWorkspacesViewModel().isDisabledProperty());

    this.languages.setItems(viewModel.getLanguagesViewModel().getItems());
    this.languages.valueProperty().bindBidirectional(viewModel.getLanguagesViewModel().selectedItemProperty());
  }

}
