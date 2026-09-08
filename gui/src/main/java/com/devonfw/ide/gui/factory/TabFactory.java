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

  private final GuiStateManager guiStateManager;
  private final NlsService nlsService;
  private final ConsoleController consoleController;
  private final CommandletService commandletService;

  private TabPane tabPane;


  public TabFactory(GuiStateManager guiStateManager, NlsService nlsService, CommandletService commandletService, ConsoleController consoleController) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.nlsService = Objects.requireNonNull(nlsService);
    this.consoleController = Objects.requireNonNull(consoleController);
    this.commandletService = commandletService;
  }


  public void attach(TabPane tabPane) {
    this.tabPane = Objects.requireNonNull(tabPane);
  }

  /**
   * Registers an action that runs before any commandlet launched from a tab (e.g. to show the console). This is a hack for the console auto-showing when
   * starting an IDE and should be removed when the console is reworked.
   *
   * @param preLaunchAction the action to run before a commandlet launches.
   */
  public void setPreLaunchAction(Runnable preLaunchAction) {
    this.commandletService.setPreLaunchAction(preLaunchAction);
  }


  public void openLauncherTab() {
    IdeLauncherViewModel viewModel = new IdeLauncherViewModel(this.guiStateManager, this.commandletService);
    open(viewModel.getTabTitleKey(), new IdeLauncherView(viewModel, this.nlsService));
  }


  public Tab open(String titleKey, Node content) {

    Tab existing = findByTitleKey(titleKey);
    if (existing != null) {
      this.tabPane.getSelectionModel().select(existing);
      return existing;
    }

    Tab tab = new Tab(this.nlsService.get(titleKey), content);
    tab.setUserData(titleKey);
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
    return this.tabPane.getTabs().stream().filter(tab -> titleKey.equals(tab.getUserData())).findFirst().orElse(null);
  }
}
