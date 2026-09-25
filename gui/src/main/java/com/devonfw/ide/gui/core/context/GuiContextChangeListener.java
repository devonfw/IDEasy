package com.devonfw.ide.gui.core.context;

/**
 * Interface that notifies listeners of context changes.
 */
public interface GuiContextChangeListener {

  /**
   * This method is called when the context changes. It can be used to update the GUI based on the new context.
   *
   * @param newContext the new {@link IdeGuiContext}.
   */
  void onContextChange(IdeGuiContext newContext);

}
