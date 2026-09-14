package com.devonfw.ide.gui.factory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.event.TabOpenEvent;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.service.NlsService;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.tab.launcher.IdeLauncherView;
import com.devonfw.ide.gui.ui.tab.launcher.IdeLauncherViewModel;

import io.github.mmm.event.EventBus;

/**
 * Creates the tabs of the main window's {@link TabPane}. It keeps at most one tab open per title key, so re-activating a tab focuses the existing one instead
 * of creating a duplicate. The managing part is to be removed in the future.
 */
public class TabFactory {

  private final GuiStateManager guiStateManager;
  private final NlsService nlsService;
  private final ConsoleController consoleController;
  private final CommandletService commandletService;
  private final EventBus eventBus;

  private final Map<String, Tab> openTabs = new LinkedHashMap<>();

  /**
   * Creates the factory.
   *
   * @param guiStateManager the app-wide selection and context holder.
   * @param nlsService the localization service.
   * @param commandletService runs commandlets on tab actions.
   * @param consoleController the controller of the console pane.
   */
  public TabFactory(GuiStateManager guiStateManager, NlsService nlsService, CommandletService commandletService,
      ConsoleController consoleController, EventBus eventBus) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.nlsService = Objects.requireNonNull(nlsService);
    this.consoleController = Objects.requireNonNull(consoleController);
    this.commandletService = Objects.requireNonNull(commandletService);
    this.eventBus = Objects.requireNonNull(eventBus);
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


  /**
   * Opens (or focuses) the IDE launcher tab.
   */
  public void openLauncherTab() {
    open(new IdeLauncherTab(guiStateManager, commandletService, nlsService, consoleController));
  }

  private void open(TabComponent tabComponent) {

    String nlsTitleKey = tabComponent.getTabTitleKey();

    Tab existing = this.openTabs.get(nlsTitleKey);
    if (existing != null) {
      this.eventBus.sendEvent(new TabOpenEvent(existing));
      return;
    }

    Tab tab = new Tab(this.nlsService.get(nlsTitleKey), tabComponent.createView());
    tab.setUserData(nlsTitleKey);
    tab.setClosable(true);
    this.openTabs.put(nlsTitleKey, tab);
    this.eventBus.sendEvent(new TabOpenEvent(tab));
  }



}
