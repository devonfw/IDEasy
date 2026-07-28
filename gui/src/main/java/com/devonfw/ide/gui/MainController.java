package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
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
  private StackPane updateIndicator;

  @FXML
  private StackPane upgradeIndicator;

  private final UpdateController updateController;

  private final UpgradeController upgradeController;

  private final NlsService nlsService;

  private final String directoryPath;

  private final Map<String, Locale> languageMap;

  /**
   * Constructor
   */
  public MainController(String directoryPath, NlsService nlsService) {
    this(directoryPath, IdeGuiStateManager.getInstance().getProjectManager(), new UpdateController(IdeGuiStateManager.getInstance(), nlsService),
        new UpgradeController(IdeGuiStateManager.getInstance(), nlsService), nlsService);
  }

  /**
   * Constructor with injected dependencies.
   *
   * @param directoryPath IDE root path
   * @param projectManager the project manager to use
   * @param updateController update controller to use for project related update actions
   * @param upgradeController upgrade controller to use for IDEasy upgrade actions
   */
  public MainController(String directoryPath, ProjectManager projectManager, UpdateController updateController, UpgradeController upgradeController,
      NlsService nlsService) {

    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;
    this.languageMap = new LinkedHashMap<>();
    this.nlsService = nlsService;
    this.projectManager = projectManager;
    this.updateController = updateController;
    this.upgradeController = upgradeController;

  }

  @FXML
  private void initialize() {
    setProjectsComboBox();
    initLanguageComboBox();
    initUpgradeAndUpdateCheck();
  }

  private void initUpgradeAndUpdateCheck() {
    try {
      this.updateController.start(this.updateIndicator);
      if (this.upgradeController != null) {
        // Pass the indicator to the upgrade controller which will manage its visibility and dialog.
        this.upgradeController.start(this.upgradeIndicator);
      }
    } catch (Exception e) {
      LOG.debug("Failed to start update controller", e);
      if (this.updateIndicator != null) {
        this.updateIndicator.setVisible(false);
      }
      if (this.upgradeController != null && this.upgradeIndicator != null) {
        // if upgrade controller failed to start, ensure indicator hidden
        this.upgradeIndicator.setVisible(false);
      }
    }
  }

  private void initLanguageComboBox() {

    this.languageMap.clear();
    selectedLanguage.getItems().clear();

    for (Locale locale : nlsService.getAvailableLocales()) {
      String displayName = nlsService.getLanguageDisplayName(locale);
      this.languageMap.put(displayName, locale);
    }

    selectedLanguage.getItems().addAll(this.languageMap.keySet());
    //initial value
    selectedLanguage.setValue(resolveLanguageSelection(nlsService.getLocale()));

    selectedLanguage.setOnAction(ev -> {
      String selection = selectedLanguage.getValue();
      Locale newLocale = this.languageMap.get(selection);
      if (newLocale != null) {
        nlsService.setLocale(newLocale);
      }
    });
  }

  private String resolveLanguageSelection(Locale currentLocale) {

    if (currentLocale == null) {
      return this.languageMap.keySet().stream().findFirst().orElse(null);
    }

    String languageTagMatch = null;
    String languageMatch = null;

    for (Map.Entry<String, Locale> entry : this.languageMap.entrySet()) {
      Locale entryLocale = entry.getValue();
      // Exact language tag match takes priority
      if (entryLocale.toLanguageTag().equalsIgnoreCase(currentLocale.toLanguageTag())) {
        return entry.getKey();
      }
      // Track language-only match as fallback
      if (languageMatch == null && entryLocale.getLanguage().equalsIgnoreCase(currentLocale.getLanguage())) {
        languageMatch = entry.getKey();
      }
    }

    // Return language-only match if found, otherwise first available
    return languageMatch != null ? languageMatch : this.languageMap.keySet().stream().findFirst().orElse(null);
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
