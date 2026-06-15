package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.i18n.I18nService;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.update.UpdateController;
import com.devonfw.ide.gui.update.UpgradeController;

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
  private Label projectHeaderLabel;

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
  private Circle upgradeIndicator;

  @FXML
  private Button updateButton;

  @FXML
  private Label updateStatusLabel;

  private final UpdateController updateController;

  private final UpgradeController upgradeController;

  private final String directoryPath;


  /**
   * Constructor
   */
  public MainController(String directoryPath) {
    this(directoryPath, IdeGuiStateManager.getInstance().getProjectManager(), new UpdateController(IdeGuiStateManager.getInstance()),
        new UpgradeController(IdeGuiStateManager.getInstance()));
  }

  /**
   * Constructor with injected project manager and update controller.
   *
   * @param directoryPath IDE root path
   * @param projectManager the project manager to use
   * @param updateController update controller to use for project related update actions
   * @param upgradeController upgrade controller to use for IDEasy upgrade actions
   */
  public MainController(String directoryPath, ProjectManager projectManager, UpdateController updateController,
      UpgradeController upgradeController) {

    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;

    this.projectManager = projectManager;
    this.updateController = updateController;
    this.upgradeController = upgradeController;
  }

  @FXML
  private void initialize() {
    setProjectsComboBox();
    initLanguageComboBox();
    updateTexts();
    I18nService.getInstance().addLocaleChangeListener(this::updateTexts);
    initUpgradeAndUpdateCheck();
  }

  private void initUpgradeAndUpdateCheck() {
    try {
      this.updateController.start(updateStatusLabel, updateButton);
      if (this.upgradeController != null) {
        // Pass the indicator to the upgrade controller which will manage its visibility and dialog.
        this.upgradeController.start(this.upgradeIndicator);
      }
    } catch (Exception e) {
      LOG.debug("Failed to start update controller", e);
      updateStatusLabel.setText(I18nService.getInstance().get("status.update.unavailable"));
      updateButton.setDisable(true);
      if (this.upgradeController != null) {
        // if upgrade controller failed to start, ensure indicator hidden
        if (this.upgradeIndicator != null) {
          this.upgradeIndicator.setVisible(false);
        }
      }
    }
  }

  @FXML
  private void dispose() {
    I18nService.getInstance().removeLocaleChangeListener(this::updateTexts);
  }

  private void initLanguageComboBox() {

    // Initialize language choices
    selectedLanguage.getItems().clear();
    selectedLanguage.getItems().addAll("English", "Deutsch");

    // Select current locale
    Locale current = I18nService.getInstance().getLocale();
    if (current != null && "de".equals(current.getLanguage())) {
      selectedLanguage.setValue("Deutsch");
    } else {
      selectedLanguage.setValue("English");
    }

    selectedLanguage.setOnAction(ev -> {
      String selection = selectedLanguage.getValue();
      Locale newLocale = "Deutsch".equals(selection) ? Locale.GERMAN : Locale.ENGLISH;
      I18nService.getInstance().setLocale(newLocale);
    });
  }

  /**
   * Use this method to update UI texts to change locale when adding new UI Elements. It uses a simple naming convention for the keys in the resource bundle.
   * Found in message.properties and message_de.properties
   */
  private void updateTexts() {
    I18nService i18n = I18nService.getInstance();
    // Set Labels
    labelProject.setText(i18n.get("label.project"));
    labelWorkspace.setText(i18n.get("label.workspace"));
    labelLanguage.setText(i18n.get("label.language"));

    // Set ComboBox prompts
    selectedProject.setPromptText(i18n.get("prompt.chooseProject"));
    selectedWorkspace.setPromptText(i18n.get("prompt.chooseWorkspace"));
    selectedLanguage.setPromptText(i18n.get("prompt.chooseLanguage"));

    // Set Button texts
    androidStudioOpen.setText(i18n.get("button.open"));
    eclipseOpen.setText(i18n.get("button.open"));
    intellijOpen.setText(i18n.get("button.open"));
    vsCodeOpen.setText(i18n.get("button.open"));
    updateButton.setText(i18n.get("button.update"));

    if (this.updateController != null) {
      this.updateController.refreshStatusText();
    }
    if (this.upgradeController != null) {
      this.upgradeController.refreshStatusText();
    }
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

  @FXML
  private void onUpdateClicked() {
    if (this.updateController != null) {
      this.updateController.onUpdateClicked();
    }
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
    selectedProject.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      projectHeaderLabel.setText(newVal != null ? newVal : "");
    });
  }

  private void setWorkspaceComboBox() {

    List<String> workspaces;
    try {
      workspaces = projectManager.getWorkspaceNames(selectedProject.getValue());
    } catch (NotDirectoryException e) {
      throw new RuntimeException(e);
    }

    selectedWorkspace.getItems().clear();
    selectedWorkspace.getItems().addAll(workspaces);

    selectedWorkspace.setOnAction(actionEvent -> {

      if (updateContext(selectedProject.getValue(), selectedWorkspace.getValue())) {
        androidStudioOpen.setDisable(false);
        eclipseOpen.setDisable(false);
        intellijOpen.setDisable(false);
        vsCodeOpen.setDisable(false);

        if (this.updateController != null) {
          this.updateController.onContextChanged(IdeGuiStateManager.getInstance().getCurrentContext());
        }
        // no-op: manual check button removed
      }
    });
  }

  private void openIDE(String inIde) {

    IdeGuiStateManager
        .getInstance()
        .getCurrentContext()
        .getCommandletManager()
        .getCommandlet(inIde)
        .run();
  }

  private boolean updateContext(String selectedProjectName, String selectedWorkspaceName) {
    try {
      IdeGuiStateManager.getInstance().switchContext(selectedProjectName, selectedWorkspaceName);
      return true;
    } catch (FileNotFoundException e) {
      IdeGuiStateManager.getInstance().clearCurrentContext();
      IdeDialog errorDialog = new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
      // no-op: manual check button removed
      return false;
    }
  }
}
