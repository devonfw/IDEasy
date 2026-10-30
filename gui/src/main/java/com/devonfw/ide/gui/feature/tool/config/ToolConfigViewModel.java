package com.devonfw.ide.gui.feature.tool.config;

import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import com.devonfw.ide.gui.core.context.GuiContextChangeListener;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.context.IdeGuiContext;
import com.devonfw.ide.gui.core.service.ToolConfiguration;
import com.devonfw.ide.gui.core.service.ToolSettingsService;
import com.devonfw.ide.gui.core.tab.TabViewModel;

/**
 * View model of the tool configuration tab. It holds the tools of the selected project with their version and edition, validates the entered versions and
 * saves the result to the project settings. The tools are reloaded whenever the selected project or workspace changes.
 */
public class ToolConfigViewModel extends TabViewModel {

  private final ToolSettingsService service;

  private final ObservableList<ToolConfiguration> tools = FXCollections.observableArrayList();

  /** Names of the tools whose entered version is invalid. */
  private final ObservableSet<String> invalidTools = FXCollections.observableSet();

  private final BooleanProperty saveDisabled = new SimpleBooleanProperty();

  private final BooleanProperty editionsLoaded = new SimpleBooleanProperty();

  private final GuiContextChangeListener contextChangeListener = _ -> load();

  private IdeGuiContext context;

  /**
   * Creates the view model and loads the tools of the currently selected project.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public ToolConfigViewModel(GuiStateManager guiStateManager) {
    super(guiStateManager);
    this.service = guiStateManager.getToolSettingsService();
    this.saveDisabled.bind(Bindings.isNotEmpty(this.invalidTools));
    this.guiStateManager.addGuiContextChangeListener(this.contextChangeListener);
    load();
  }

  @Override
  public String getTabTitleKey() {
    return "tab.tool_config";
  }

  /**
   * Loads the tools of the currently selected project. The editions are loaded afterwards in the background, as each lookup is a directory scan.
   */
  public void load() {

    this.context = this.guiStateManager.getCurrentContext();
    this.invalidTools.clear();
    this.editionsLoaded.set(false);
    if (this.context == null) {
      this.tools.clear();
      return;
    }
    List<ToolConfiguration> configurations = this.service.listToolConfigurations(this.context);
    this.tools.setAll(configurations);

    IdeGuiContext loadContext = this.context;
    runInBackground(() -> {
      for (ToolConfiguration tool : configurations) {
        List<String> editions = this.service.loadEditionsForTool(tool.getToolName(), loadContext);
        tool.setAvailableEditions(editions);
        // only show the edition selector when there is a real choice
        tool.setSupportsEdition(editions.size() > 1);
        // auto-select the sole edition so the user does not have to
        if (editions.size() == 1 && isBlank(tool.getConfiguredEdition())) {
          tool.setConfiguredEdition(editions.getFirst());
        }
      }
    }, () -> this.editionsLoaded.set(true));
  }

  /**
   * Loads the available versions of the given tool for its configured edition in the background and caches them on the tool.
   *
   * @param tool the {@link ToolConfiguration}.
   * @param onLoaded called on the JavaFX thread once {@link ToolConfiguration#getAvailableVersions()} has been updated.
   */
  public void loadVersions(ToolConfiguration tool, Runnable onLoaded) {

    if (this.context == null) {
      return;
    }
    IdeGuiContext loadContext = this.context;
    String edition = tool.getConfiguredEdition();
    runInBackground(() -> tool.setAvailableVersions(this.service.loadVersionsForSelectedEdition(tool.getToolName(), edition, loadContext)), onLoaded);
  }

  /**
   * Sets the version entered by the user and validates it against the available versions. A blank version is normalized to {@code *} (latest).
   *
   * @param tool the {@link ToolConfiguration}.
   * @param version the entered version.
   * @param onValidated called on the JavaFX thread once {@link #isInvalid(ToolConfiguration)} has been updated.
   */
  public void changeVersion(ToolConfiguration tool, String version, Runnable onValidated) {

    tool.setConfiguredVersion(isBlank(version) ? "*" : version);
    if (tool.getAvailableVersions() != null) {
      validate(tool);
      onValidated.run();
    } else {
      loadVersions(tool, () -> {
        validate(tool);
        onValidated.run();
      });
    }
  }

  /**
   * Sets the edition selected by the user, reloads the available versions for it and re-validates the entered version, as a version valid for the old edition
   * may not exist for the new one.
   *
   * @param tool the {@link ToolConfiguration}.
   * @param edition the selected edition.
   * @param onUpdated called on the JavaFX thread once the versions have been reloaded and validated.
   */
  public void changeEdition(ToolConfiguration tool, String edition, Runnable onUpdated) {

    if (isBlank(edition)) {
      return;
    }
    tool.setConfiguredEdition(edition);
    loadVersions(tool, () -> {
      validate(tool);
      onUpdated.run();
    });
  }

  private void validate(ToolConfiguration tool) {

    if (this.service.isValidVersion(tool.getConfiguredVersion(), tool.getAvailableVersions())) {
      this.invalidTools.remove(tool.getToolName());
    } else {
      this.invalidTools.add(tool.getToolName());
    }
  }

  /**
   * @param tool the {@link ToolConfiguration}.
   * @return {@code true} if the version entered for the given tool is invalid, {@code false} otherwise.
   */
  public boolean isInvalid(ToolConfiguration tool) {

    return this.invalidTools.contains(tool.getToolName());
  }

  /**
   * @return the {@code ide.properties} content that would result from saving the current configuration.
   */
  public String getPreview() {

    return this.service.buildPreviewSettingsContent(this.tools);
  }

  /**
   * Saves the current configuration to the {@code ide.properties} of the project settings.
   */
  public void save() {

    this.service.applyAndSave(this.tools, this.context);
  }

  /**
   * Stops reacting to context changes. Called when the tab is closed.
   */
  public void dispose() {

    this.guiStateManager.removeGuiContextChangeListener(this.contextChangeListener);
  }

  public ObservableList<ToolConfiguration> getTools() {
    return this.tools;
  }

  public BooleanProperty saveDisabledProperty() {
    return this.saveDisabled;
  }

  public BooleanProperty editionsLoadedProperty() {
    return this.editionsLoaded;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static void runInBackground(Runnable task, Runnable onFinished) {

    Thread thread = new Thread(() -> {
      task.run();
      Platform.runLater(onFinished);
    });
    thread.setDaemon(true);
    thread.start();
  }

}
