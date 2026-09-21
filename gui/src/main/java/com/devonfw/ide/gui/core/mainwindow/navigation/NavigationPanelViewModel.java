package com.devonfw.ide.gui.core.mainwindow.navigation;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.scene.control.Alert.AlertType;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.context.ProjectManager;
import com.devonfw.ide.gui.core.helper.combobox.ComboBoxViewModel;
import com.devonfw.ide.gui.core.modal.IdeDialog;
import com.devonfw.ide.gui.core.service.NlsService;

public class NavigationPanelViewModel {

  private final ComboBoxViewModel<String> projectsViewModel = new ComboBoxViewModel<>();

  private final ComboBoxViewModel<String> workspacesViewModel = new ComboBoxViewModel<>();

  private final ComboBoxViewModel<String> languagesViewModel = new ComboBoxViewModel<>();

  private final Map<String, Locale> languageMap = new LinkedHashMap<>();

  private final GuiStateManager guiStateManager;

  private final ProjectManager projectManager;

  private final NlsService nlsService;

  public NavigationPanelViewModel(GuiStateManager guiStateManager, NlsService nlsService) {
    this.guiStateManager = guiStateManager;
    this.projectManager = guiStateManager.getProjectManager();
    this.nlsService = nlsService;

    initProjectsComboBox();
    initLanguageComboBox();
  }

  private void initProjectsComboBox() {
    this.projectsViewModel.getItems().clear();
    this.projectsViewModel.getItems().addAll(this.projectManager.getProjectNames());
  }

  public void populateWorkspaceComboBox() {

    List<String> loadedWorkspaces = loadWorkspaces(this.projectsViewModel.getSelectedItem().orElse(null));

    this.workspacesViewModel.clearSelection();
    this.workspacesViewModel.getItems().clear();
    this.workspacesViewModel.getItems().addAll(loadedWorkspaces);

    // Default to the "main" workspace when it exists.
    if (loadedWorkspaces.contains(GuiStateManager.DEFAULT_WORKSPACE)) {
      this.workspacesViewModel.select(GuiStateManager.DEFAULT_WORKSPACE);
    }
  }

  private List<String> loadWorkspaces(String project) {

    try {
      return this.projectManager.getWorkspaceNames(project);
    } catch (NotDirectoryException e) {
      throw new RuntimeException(e);
    }
  }

  private void initLanguageComboBox() {

    this.languageMap.clear();
    this.languagesViewModel.getItems().clear();

    for (Locale locale : this.nlsService.getAvailableLocales()) {
      String displayName = this.nlsService.getLanguageDisplayName(locale);
      this.languageMap.put(displayName, locale);
    }

    this.languagesViewModel.getItems().addAll(this.languageMap.keySet());
    //initial value
    this.languagesViewModel.select(resolveLanguageSelection(this.nlsService.getLocale()));
  }

  private String resolveLanguageSelection(Locale currentLocale) {

    if (currentLocale == null) {
      return this.languageMap.keySet().stream().findFirst().orElse(null);
    }

    String languageMatch = null;

    for (Map.Entry<String, Locale> entry : this.languageMap.entrySet()) {
      Locale entryLocale = entry.getValue();
      // Exact language tag match takes priority
      if (entryLocale.toLanguageTag().equalsIgnoreCase(currentLocale.toLanguageTag())) {
        return entry.getKey();
      }
      // Track language-only match as fallback
      if (languageMatch == null) {
        if (entryLocale.getLanguage().equalsIgnoreCase(currentLocale.getLanguage())) {
          languageMatch = entry.getKey();
        }
      }
    }

    // Return language-only match if found, otherwise first available
    return languageMatch != null ? languageMatch : this.languageMap.keySet().stream().findFirst().orElse(null);
  }

  public void updateContext() {

    String project = this.projectsViewModel.getSelectedItem().orElse(null);
    String workspace = this.workspacesViewModel.getSelectedItem().orElse(null);

    if (project == null || workspace == null) {
      this.guiStateManager.workspaceSelectedProperty().set(false);
      return;
    }

    try {
      this.guiStateManager.switchContext(project, workspace);
    } catch (FileNotFoundException e) {
      IdeDialog errorDialog = new IdeDialog(AlertType.ERROR, e.getMessage());
      errorDialog.showAndWait();
    }
  }

  public ComboBoxViewModel<String> getProjectsViewModel() {
    return projectsViewModel;
  }

  public ComboBoxViewModel<String> getWorkspacesViewModel() {
    return workspacesViewModel;
  }

  public ComboBoxViewModel<String> getLanguagesViewModel() {
    return languagesViewModel;
  }

}
