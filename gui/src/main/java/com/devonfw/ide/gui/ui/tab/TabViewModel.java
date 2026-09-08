package com.devonfw.ide.gui.ui.tab;

import com.devonfw.ide.gui.context.GuiStateManager;

/**
 * Base for the view models of all tabs.
 */
public abstract class TabViewModel {

  protected final GuiStateManager guiStateManager;

  protected TabViewModel(GuiStateManager guiStateManager) {
    this.guiStateManager = guiStateManager;
  }

  public abstract String getTabTitleKey();

}
