package com.devonfw.ide.gui.ui.tab;

import javafx.scene.control.Tab;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.event.TabChangeEvent;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleView;
import com.devonfw.ide.gui.ui.controls.console.ConsoleViewModel;

/**
 * Base class for the tabs of the main window. Each tab combines a {@link TabViewModel} with a {@link TabView} that renders it.
 */
public abstract class TabComponent<T extends TabViewModel> {

  protected final GuiStateManager guiStateManager;
  protected final CommandletService commandletService;
  protected final NlsService nlsService;
  protected ConsoleViewModel consoleViewModel;

  protected Tab tab;
  protected TabView view;
  protected T viewModel;

  /**
   * Creates the tab.
   *
   * @param guiStateManager the app-wide selection and context holder.
   * @param commandletService the service that runs commandlets.
   * @param nlsService the localization service.
   * @param consoleViewModel the view model of the console pane.
   */
  public TabComponent(GuiStateManager guiStateManager, CommandletService commandletService, NlsService nlsService, ConsoleViewModel consoleViewModel) {
    this.consoleViewModel = consoleViewModel;
    this.commandletService = commandletService;
    this.nlsService = nlsService;
    this.guiStateManager = guiStateManager;
  }

  /**
   * Creates the tab's view and its view model.
   *
   * @return the created view.
   */
  public abstract TabView createTab();

  public abstract TabChangeEvent createTabChangeEvent();

  /**
   * @return the localization key used as the title (and identity) of this tab.
   */
  public abstract String getTabTitleKey();

  /**
   * @return the view created by {@link #createView()}.
   */
  public abstract TabView getView();

  /**
   * @return the view model created by {@link #createView()}.
   */
  public abstract T getViewModel();
}
