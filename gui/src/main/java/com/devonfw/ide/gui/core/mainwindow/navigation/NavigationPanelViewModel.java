package com.devonfw.ide.gui.core.mainwindow.navigation;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

  private final BooleanProperty restartRequired = new SimpleBooleanProperty(false);

  private final GuiStateManager guiStateManager;

  private final ProjectManager projectManager;

  private final NlsService nlsService;

  public NavigationPanelViewModel(GuiStateManager guiStateManager) {
    this.guiStateManager = guiStateManager;
    this.projectManager = guiStateManager.getProjectManager();
    this.nlsService = guiStateManager.getNlsService();

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

  /**
   * Resolves the language combo label for the given locale. The applied locale equals the JVM default (see {@code NlsService}), which is often a regional
   * variant (e.g. {@code de-DE}) while the available bundles are language-only (e.g. {@code de}). An exact language-tag match takes priority, a language-only
   * match is the fallback, and the first available label is the last resort so the combo never disagrees with the displayed language.
   */
  private String resolveLanguageSelection(Locale currentLocale) {

    String firstAvailable = this.languageMap.keySet().stream().findFirst().orElse(null);
    if (currentLocale == null) {
      return firstAvailable;
    }
    String languageMatch = null;
    for (Map.Entry<String, Locale> entry : this.languageMap.entrySet()) {
      Locale entryLocale = entry.getValue();
      // Exact language tag match takes priority (e.g. a persisted de-DE selecting the de-DE entry)
      if (entryLocale.toLanguageTag().equalsIgnoreCase(currentLocale.toLanguageTag())) {
        return entry.getKey();
      }
      // Track a language-only match as fallback (e.g. the JVM default de-DE selecting the language-only de entry)
      if (languageMatch == null && entryLocale.getLanguage().equalsIgnoreCase(currentLocale.getLanguage())) {
        languageMatch = entry.getKey();
      }
    }
    return languageMatch != null ? languageMatch : firstAvailable;
  }

  /**
   * Selects the language with the given combo label. If it differs from the currently applied locale the selection is persisted (it takes effect on the next
   * GUI start) and the restart-required flag is raised so the view can prompt the user to restart. Selecting the already-active locale is a no-op.
   *
   * @param displayLabel the language combo label, as produced by {@code NlsService.getLanguageDisplayName}.
   */
  public void selectLanguage(String displayLabel) {

    Locale locale = this.languageMap.get(displayLabel);
    if (locale == null || locale.equals(this.nlsService.getLocale())) {
      return;
    }
    this.nlsService.setPersistedLocale(locale.toLanguageTag());
    this.restartRequired.set(true);
  }

  /**
   * @return whether the GUI must be restarted for the selected language to take effect.
   */
  public boolean isRestartRequired() {

    return this.restartRequired.get();
  }

  /**
   * @return the property flagging that a restart is required for the selected language to take effect. The view observes it to prompt the user and resets it
   *         after showing the prompt.
   */
  public BooleanProperty restartRequiredProperty() {

    return this.restartRequired;
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
