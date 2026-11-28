package com.devonfw.ide.gui.feature.tool.config;

import javafx.scene.control.Tab;

import com.devonfw.ide.gui.core.context.GuiStateManager;
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
  public Tab createTab() {
    this.viewModel = new ToolConfigViewModel(this.guiStateManager);
    this.view = new ToolConfigView(this.viewModel, this.nlsService);
    Tab tab = new Tab(this.nlsService.get(tabTitleKey), this.view);
    tab.setOnClosed(_ -> this.viewModel.dispose());

    return tab;
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
