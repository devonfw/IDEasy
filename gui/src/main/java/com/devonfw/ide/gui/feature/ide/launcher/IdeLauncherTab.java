package com.devonfw.ide.gui.feature.ide.launcher;

import javafx.scene.control.Tab;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.event.TabChangeEvent;
import com.devonfw.ide.gui.core.mainwindow.console.ConsoleController;
import com.devonfw.ide.gui.core.service.CommandletService;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.ide.gui.core.tab.TabComponent;
import com.devonfw.ide.gui.core.tab.TabView;

/**
 * Tab that lets the user launch the supported IDEs from the main window.
 */
public class IdeLauncherTab extends TabComponent<IdeLauncherViewModel> {

  private final String tabTitleKey = "tab.ide_launcher";

  /**
   * Creates the IDE launcher tab.
   *
   * @param guiStateManager the app-wide selection and context holder.
   * @param commandletService the service that runs commandlets.
   * @param nlsService the localization service.
   * @param consoleController the controller of the console pane.
   */
  public IdeLauncherTab(GuiStateManager guiStateManager, CommandletService commandletService, NlsService nlsService, ConsoleController consoleController) {
    super(guiStateManager, commandletService, nlsService, consoleController);
  }

  @Override
  public TabView createTab() {
    this.viewModel = new IdeLauncherViewModel(this.guiStateManager, this.commandletService);
    this.view = new IdeLauncherView(this.viewModel, this.nlsService);
    this.tab = new Tab(this.nlsService.get(tabTitleKey), this.view);
    this.tab.setUserData(tabTitleKey);
    this.tab.setClosable(true);

    return this.view;
  }

  @Override
  public TabChangeEvent createTabChangeEvent() {
    createTab();
    return new TabChangeEvent(this.tab);
  }

  @Override
  public String getTabTitleKey() {
    return this.tabTitleKey;
  }

  @Override
  public TabView getView() {
    return view;
  }

  @Override
  public IdeLauncherViewModel getViewModel() {
    return viewModel;
  }

}
