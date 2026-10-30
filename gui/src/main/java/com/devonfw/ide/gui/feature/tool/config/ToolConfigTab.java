package com.devonfw.ide.gui.feature.tool.config;

import javafx.scene.control.Tab;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.event.TabChangeEvent;
import com.devonfw.ide.gui.core.tab.TabComponent;
import com.devonfw.ide.gui.core.tab.TabView;

/**
 * Tab that lets the user select the tools of the current project and configure their version and edition.
 */
public class ToolConfigTab extends TabComponent<ToolConfigViewModel> {

  private final String tabTitleKey = "tab.tool_config";

  /**
   * Creates the tool configuration tab.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public ToolConfigTab(GuiStateManager guiStateManager) {
    super(guiStateManager);
  }

  @Override
  public TabView createTab() {
    this.viewModel = new ToolConfigViewModel(this.guiStateManager);
    this.view = new ToolConfigView(this.viewModel, this.nlsService);
    this.tab = new Tab(this.nlsService.get(tabTitleKey), this.view);
    this.tab.setUserData(tabTitleKey);
    this.tab.setOnClosed(_ -> this.viewModel.dispose());

    return this.view;
  }

  @Override
  public TabChangeEvent createTabChangeEvent() {
    // create the tab only once so re-opening an already open tab just focuses it
    if (this.tab == null) {
      createTab();
    }
    return new TabChangeEvent(this.tab);
  }

  /**
   * @return {@code true} if the tab has been created and is currently shown in the main window, {@code false} otherwise.
   */
  public boolean isOpen() {
    return this.tab != null && this.tab.getTabPane() != null;
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
  public ToolConfigViewModel getViewModel() {
    return viewModel;
  }

}
