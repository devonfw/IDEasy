package com.devonfw.ide.gui.core.tab;

import com.devonfw.ide.gui.core.context.GuiStateManager;

/**
 * Base for the view models of all tabs.
 */
public abstract class TabViewModel {

  protected final GuiStateManager guiStateManager;

  protected TabViewModel(GuiStateManager guiStateManager) {
    this.guiStateManager = guiStateManager;
  }

  /**
   * @return the localization key used as the title of this tab.
   */
  public abstract String getTabTitleKey();

}
