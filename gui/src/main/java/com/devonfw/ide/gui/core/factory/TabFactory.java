package com.devonfw.ide.gui.core.factory;

import java.util.Objects;

import javafx.scene.control.TabPane;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.mainwindow.console.ConsoleController;
import com.devonfw.ide.gui.core.service.CommandletService;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.ide.gui.core.tab.TabComponent;
import com.devonfw.ide.gui.feature.ide.launcher.IdeLauncherTab;

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

  /**
   * Creates the factory.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public TabFactory(GuiStateManager guiStateManager) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.nlsService = Objects.requireNonNull(guiStateManager.getNlsService());
    this.consoleController = Objects.requireNonNull(guiStateManager.getConsoleController());
    this.commandletService = Objects.requireNonNull(guiStateManager.getCommandletService());
    this.eventBus = Objects.requireNonNull(guiStateManager.getEventBus());
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
    this.eventBus.sendEvent(tabComponent.createTabChangeEvent());
  }

}
