package com.devonfw.ide.gui.ui.tab;

import com.devonfw.ide.gui.context.GuiStateManager;

/**
 * Base for the view models of all logic-bearing tabs. Every tab view model reads the shared session selection off the {@link GuiStateManager}, so this holds
 * that dependency in one canonical place; concrete tab view models extend this.
 */
public abstract class TabViewModel {

  /** The shared session state (project/workspace selection) that every tab view model reads. */
  protected final GuiStateManager guiStateManager;

  protected TabViewModel(GuiStateManager guiStateManager) {
    this.guiStateManager = guiStateManager;
  }

}
