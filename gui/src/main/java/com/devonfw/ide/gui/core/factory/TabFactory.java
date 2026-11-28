package com.devonfw.ide.gui.core.factory;

import java.util.Objects;

import javafx.scene.control.TabPane;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.event.TabChangeEvent;
import com.devonfw.ide.gui.core.service.CommandletService;
import com.devonfw.ide.gui.core.tab.TabComponent;
import com.devonfw.ide.gui.feature.ide.launcher.IdeLauncherTab;
import com.devonfw.ide.gui.feature.tool.config.ToolConfigTab;

import io.github.mmm.event.EventBus;

/**
 * Sends requests to open tabs in the main window's {@link TabPane}.
 */
public class TabFactory {

  private final GuiStateManager guiStateManager;
  private final CommandletService commandletService;
  private final EventBus eventBus;

  /**
   * Creates the factory.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public TabFactory(GuiStateManager guiStateManager) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
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
    open(new IdeLauncherTab(guiStateManager));
  }

  /**
   * Opens the tool configuration tab.
   */
  public void openToolConfigTab() {
    open(new ToolConfigTab(this.guiStateManager));
  }

  private void open(TabComponent<?> tabComponent) {
    this.eventBus.sendEvent(new TabChangeEvent(tabComponent));
  }

}
