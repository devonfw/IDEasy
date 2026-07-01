package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.localization.LocalizationService;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.settings.ToolSettingsController;

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
  private TabPane tabPane;

  @FXML
  private Tab mainTab;

  @FXML
  private Tab toolConfigTab;

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
    toolConfigTab.setDisable(true);
    toolConfigTab.setTooltip(new Tooltip(LocalizationService.getInstance().get("tooltip.toolConfigDisabled")));
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
      if (newTab == toolConfigTab) {
        loadToolConfigContent();
      }
    });
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
      toolConfigTab.setDisable(false);
      // If tool config is already open, reload it to reflect the new context
      if (toolConfigTab.isSelected()) {
        loadToolConfigContent();
      }
      // Pre-warm ide-urls git repo in background so the first dropdown open is fast
      Thread preWarm = new Thread(() -> {
        try {
          IdeGuiStateManager.getInstance().getCurrentContext().getUrls();
        } catch (Exception ignored) {
        }
      });
      preWarm.setDaemon(true);
      preWarm.start();
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

  private void loadToolConfigContent() {

    try {
      ToolSettingsController controller = new ToolSettingsController();
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devonfw/ide/gui/tools-config.fxml"));
      loader.setController(controller);
      loader.setResources(LocalizationService.getInstance().getResourceBundle());
      Parent content = loader.load();
      controller.setOnClose(() -> tabPane.getSelectionModel().select(mainTab));
      toolConfigTab.setContent(content);
    } catch (Exception e) {
      LOG.error("Failed to load tool config view", e);
      new IdeDialog(IdeDialog.AlertType.ERROR, e.getMessage()).showAndWait();
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
