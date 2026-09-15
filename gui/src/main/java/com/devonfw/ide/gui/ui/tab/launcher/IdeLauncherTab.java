package com.devonfw.ide.gui.ui.tab.launcher;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.tab.TabComponent;
import com.devonfw.ide.gui.ui.tab.TabView;

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
  public TabView createView() {
    this.viewModel = new IdeLauncherViewModel(this.guiStateManager, this.commandletService);
    this.view = new IdeLauncherView(this.viewModel, this.nlsService);

    return this.view;
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
