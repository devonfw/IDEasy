package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.localization.LocalizationService;
import com.devonfw.ide.gui.modal.IdeDialog;

/**
 * Controller of the main screen of the dashboard GUI.
 */
@SuppressWarnings("unused")
public class MainController {

  private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

  private final ProjectManager projectManager;

  @FXML
  private ComboBox<String> selectedProject;

  @FXML
  private ComboBox<String> selectedWorkspace;

  @FXML
  private Label labelProject;

  @FXML
  private Label labelWorkspace;

  @FXML
  private Label labelLanguage;

  @FXML
  private ComboBox<String> selectedLanguage;

  @FXML
  private Button androidStudioOpen;

  @FXML
  private Button eclipseOpen;

  @FXML
  private Button intellijOpen;

  @FXML
  private Button vsCodeOpen;

  @FXML
  private Button toolsConfigButton;

  private final String directoryPath;


  /**
   * Constructor
   */
  public MainController(String directoryPath) {

    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;

    this.projectManager = IdeGuiStateManager.getInstance().getProjectManager();
  }

  @FXML
  private void initialize() {
    setProjectsComboBox();
    initLanguageComboBox();
    updateTexts();
    LocalizationService.getInstance().addLocaleChangeListener(this::updateTexts);
    // Disable tools config until a project and workspace are selected (context must be available)
    updateToolsConfigButtonState();
  }

  @FXML
  private void dispose() {
    LocalizationService.getInstance().removeLocaleChangeListener(this::updateTexts);
  }

  private void initLanguageComboBox() {

    // Initialize language choices
    selectedLanguage.getItems().clear();
    selectedLanguage.getItems().addAll("English", "Deutsch");

    // Select current locale
    Locale current = LocalizationService.getInstance().getLocale();
    if (current != null && "de".equals(current.getLanguage())) {
      selectedLanguage.setValue("Deutsch");
    } else {
      selectedLanguage.setValue("English");
    }

    selectedLanguage.setOnAction(ev -> {
      String selection = selectedLanguage.getValue();
      Locale newLocale = "Deutsch".equals(selection) ? Locale.GERMAN : Locale.ENGLISH;
      LocalizationService.getInstance().setLocale(newLocale);
    });
  }

  /**
   * Use this method to update UI texts to change locale when adding new UI Elements. It uses a simple naming convention for the keys in the resource bundle.
   * Found in message.properties and message_de.properties
   */
  private void updateTexts() {
    LocalizationService localizationService = LocalizationService.getInstance();
    // Set Labels
    labelProject.setText(localizationService.get("label.project"));
    labelWorkspace.setText(localizationService.get("label.workspace"));
    labelLanguage.setText(localizationService.get("label.language"));

    // Set ComboBox prompts
    selectedProject.setPromptText(localizationService.get("prompt.chooseProject"));
    selectedWorkspace.setPromptText(localizationService.get("prompt.chooseWorkspace"));
    selectedLanguage.setPromptText(localizationService.get("prompt.chooseLanguage"));

    // Set Button texts
    androidStudioOpen.setText(localizationService.get("button.open"));
    eclipseOpen.setText(localizationService.get("button.open"));
    intellijOpen.setText(localizationService.get("button.open"));
    vsCodeOpen.setText(localizationService.get("button.open"));
    toolsConfigButton.setText(localizationService.get("button.toolsConfig"));
  }


  @FXML
  private void openAndroidStudio() {

    openIDE("android-studio");
  }

  @FXML
  private void openEclipse() {

    openIDE("eclipse");
  }

  @FXML
  private void openIntellij() {

    openIDE("intellij");
  }

  @FXML
  private void openVsCode() {

    openIDE("vscode");
  }


  private void setProjectsComboBox() {

    assert (directoryPath != null) : "directoryPath is null! Please check the setup of your environment variables (IDE_ROOT)";

    List<String> projects = projectManager.getProjectNames();

    selectedProject.getItems().clear();
    selectedProject.getItems().addAll(projects);

    selectedProject.setOnAction(actionEvent -> {

      setWorkspaceComboBox();

      selectedWorkspace.setDisable(false);

    });
  }

  private void setWorkspaceComboBox() {

    List<String> workspaces = null;
    try {
      workspaces = projectManager.getWorkspaceNames(selectedProject.getValue());
    } catch (NotDirectoryException e) {
      throw new RuntimeException(e);
    }

    selectedWorkspace.getItems().clear();
    selectedWorkspace.getItems().addAll(workspaces);

    selectedWorkspace.setOnAction(actionEvent -> {
      updateContext(selectedProject.getValue(), selectedWorkspace.getValue());

      androidStudioOpen.setDisable(false);
      eclipseOpen.setDisable(false);
      intellijOpen.setDisable(false);
      vsCodeOpen.setDisable(false);
      // Now we have a workspace selected -> enable tools config
      updateToolsConfigButtonState();
    });
  }

  /**
   * Enable or disable the tools config button depending on whether a project and workspace are selected.
   */
  private void updateToolsConfigButtonState() {
    boolean enabled = false;
    try {
      enabled = selectedProject != null && selectedProject.getValue() != null && selectedWorkspace != null
          && selectedWorkspace.getValue() != null;
    } catch (Exception e) {
      enabled = false;
    }
    if (toolsConfigButton != null) {
      toolsConfigButton.setDisable(!enabled);
    }
  }

  private void openIDE(String inIde) {

    IdeGuiStateManager
        .getInstance()
        .getCurrentContext()
        .getCommandletManager()
        .getCommandlet(inIde)
        .run();
  }

  @FXML
  private void openToolsConfig() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devonfw/ide/gui/tools-config.fxml"));
      loader.setResources(LocalizationService.getInstance().getResourceBundle());
      Parent root = loader.load();
      Stage dialog = new Stage();
      dialog.initModality(Modality.APPLICATION_MODAL);
      dialog.initOwner(this.selectedProject.getScene().getWindow());
      dialog.setTitle(LocalizationService.getInstance().get("label.toolsConfig"));
      dialog.setScene(new Scene(root));
      dialog.showAndWait();
    } catch (Exception e) {
      LOG.error("Failed to open tools configuration dialog", e);
      IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  private void updateContext(String selectedProjectName, String selectedWorkspaceName) {
    try {
      IdeGuiStateManager.getInstance().switchContext(selectedProjectName, selectedWorkspaceName);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }
}
