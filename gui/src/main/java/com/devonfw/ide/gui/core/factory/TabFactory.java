package com.devonfw.ide.gui.core.factory;

import java.util.Objects;

import javafx.scene.control.TabPane;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.service.CommandletService;
import com.devonfw.ide.gui.core.tab.TabComponent;
import com.devonfw.ide.gui.feature.ide.launcher.IdeLauncherTab;
import com.devonfw.ide.gui.feature.tool.config.ToolConfigTab;

import io.github.mmm.event.EventBus;

/**
 * Creates the tabs of the main window's {@link TabPane}. It keeps at most one tab open per title key, so re-activating a tab focuses the existing one instead
 * of creating a duplicate. The managing part is to be removed in the future.
 */
public class TabFactory {

  private final GuiStateManager guiStateManager;
  private final CommandletService commandletService;
  private final EventBus eventBus;
  private ToolConfigTab toolConfigTab;

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
   * Opens the tool configuration tab, or focuses it if it is already open.
   */
  public void openToolConfigTab() {
    if (this.toolConfigTab == null || !this.toolConfigTab.isOpen()) {
      this.toolConfigTab = new ToolConfigTab(this.guiStateManager);
    }
    open(this.toolConfigTab);
  }

  private void open(TabComponent tabComponent) {
    this.eventBus.sendEvent(tabComponent.createTabChangeEvent());
  }

}
