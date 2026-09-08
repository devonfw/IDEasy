package com.devonfw.ide.gui.ui.controls.mainwindow;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.ui.mainwindow.MainWindow;

/**
 * Controller for the main-window navigation panel (the left project/workspace/language sidebar). It is wired in from {@code MainWindow.fxml} via
 * {@code fx:controller} and {@link FXMLLoader#setControllerFactory javafx.fxml.FXMLLoader#setControllerFactory}; the shared selection is kept in sync with the
 * {@link GuiStateManager}.
 */
public class NavigationPanelControl extends VBox {

  @FXML
  private ComboBox<String> projects;

  @FXML
  private ComboBox<String> workspaces;

  @FXML
  private ComboBox<String> languages;

  public NavigationPanelControl() {
    super();

    final FXMLLoader loader = new FXMLLoader(getClass().getResource("NavigationPanelControl.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    // This control self-loads its FXML in a no-arg constructor, so it has no NlsService reference here. The FXML uses %key text, so it needs a
    // ResourceBundle to resolve them. Resolve the same default-locale bundle the app loads at startup (see NlsService). To be replaced by the shared
    // NlsService once it is reworked into a singleton (follow-up issue).
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

  public void bind(MainWindow mainWindow) {
    projects.setItems(mainWindow.getProjectsViewModel().getItems());
    projects.valueProperty().bindBidirectional(mainWindow.getProjectsViewModel().selectedItemProperty());

    workspaces.setItems(mainWindow.getWorkspacesViewModel().getItems());
    workspaces.valueProperty().bindBidirectional(mainWindow.getWorkspacesViewModel().selectedItemProperty());
    workspaces.disableProperty().bind(mainWindow.getWorkspacesViewModel().isDisabledProperty());
    workspaces.getSelectionModel()
        .selectedItemProperty()
        .addListener(((observable, oldValue, newValue) -> {
          if (newValue == null) {
            mainWindow.setIsWorkspaceSelected(false);
          } else {
            mainWindow.setIsWorkspaceSelected(true);
            mainWindow.updateContext();
          }
        }));

    languages.setItems(mainWindow.getLanguagesViewModel().getItems());
    languages.valueProperty().bindBidirectional(mainWindow.getLanguagesViewModel().selectedItemProperty());

    this.projects.setOnAction(_ -> mainWindow.populateWorkspaceComboBox());
  }

}
