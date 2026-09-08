package com.devonfw.ide.gui.factory;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.tab.launcher.IdeLauncherView;
import com.devonfw.ide.gui.ui.tab.launcher.IdeLauncherViewModel;


public class TabFactory {

  /** Localization key for the IDE launcher tab title. */
  public static final String TAB_KEY_IDE_LAUNCHER = "tab.ide_launcher";

  private final GuiStateManager guiStateManager;
  private final NlsService nlsService;
  private final ConsoleController consoleController;
  private final CommandletService commandletService;

  private TabPane tabPane;


  public TabFactory(GuiStateManager guiStateManager, NlsService nlsService, ConsoleController consoleController) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.nlsService = Objects.requireNonNull(nlsService);
    this.consoleController = Objects.requireNonNull(consoleController);
    this.commandletService = new CommandletService(guiStateManager, consoleController);
  }


  public void attach(TabPane tabPane) {
    this.tabPane = Objects.requireNonNull(tabPane);
  }

  /**
   * Registers an action that runs before any commandlet launched from a tab (e.g. to show the console).
   *
   * @param preLaunchAction the action to run before a commandlet launches.
   */
  public void setPreLaunchAction(Runnable preLaunchAction) {
    this.commandletService.setPreLaunchAction(preLaunchAction);
  }


  public void openLauncherTab() {
    open(TAB_KEY_IDE_LAUNCHER,
        new IdeLauncherView(new IdeLauncherViewModel(this.guiStateManager, this.commandletService), this.nlsService));
  }


  public Tab open(String titleKey, Node content) {

    Tab existing = findByTitleKey(titleKey);
    if (existing != null) {
      this.tabPane.getSelectionModel().select(existing);
      return existing;
    }

    Tab tab = new Tab(this.nlsService.get(titleKey), content);
    tab.setClosable(true);
    this.tabPane.getTabs().add(tab);
    return tab;
  }

  public void close(Tab tab) {
    this.tabPane.getTabs().remove(tab);
  }


  public void focus(Tab tab) {
    this.tabPane.getSelectionModel().select(tab);
  }


  public boolean isOpen(String titleKey) {
    return findByTitleKey(titleKey) != null;
  }

  public TabPane getTabPane() {
    return this.tabPane;
  }

  public ConsoleController getConsoleController() {
    return this.consoleController;
  }

  private Tab findByTitleKey(String titleKey) {
    String title = this.nlsService.get(titleKey);
    return this.tabPane.getTabs().stream().filter(tab -> title.equals(tab.getText())).findFirst().orElse(null);
  }
}
