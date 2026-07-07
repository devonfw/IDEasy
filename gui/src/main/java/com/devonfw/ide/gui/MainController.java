package com.devonfw.ide.gui;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.IdeGuiStateManager;
import com.devonfw.ide.gui.context.ProjectManager;
import com.devonfw.ide.gui.modal.IdeDialog;
import com.devonfw.ide.gui.nls.NlsService;
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

  private final Map<String, Locale> languageMap;

  private final NlsService nlsService;


  /**
   * Constructor
   */
  public MainController(String directoryPath, NlsService nlsService) {

    LOG.debug("IDE_ROOT path={}", directoryPath);
    this.directoryPath = directoryPath;
    this.languageMap = new LinkedHashMap<>();
    this.nlsService = nlsService;

    this.projectManager = IdeGuiStateManager.getInstance().getProjectManager();
  }

  @FXML
  private void initialize() {
    setProjectsComboBox();
    initLanguageComboBox();
    toolConfigTab.setDisable(true);
    toolConfigTab.setTooltip(new Tooltip(nlsService.get("toolConfigDisabled")));
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
      if (newTab == toolConfigTab) {
        loadToolConfigContent();
      }
    });
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
      ToolSettingsController controller = new ToolSettingsController(nlsService);
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devonfw/ide/gui/tools-config.fxml"));
      loader.setController(controller);
      loader.setResources(nlsService.getResourceBundle());
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
