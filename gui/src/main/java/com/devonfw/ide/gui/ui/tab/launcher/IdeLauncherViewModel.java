package com.devonfw.ide.gui.ui.tab.launcher;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.service.CommandletService;
import com.devonfw.ide.gui.ui.tab.TabViewModel;

public class IdeLauncherViewModel extends TabViewModel {

  private final CommandletService commandletService;

  private final BooleanProperty isIdeButtonsDisabledProperty = new SimpleBooleanProperty();

  public IdeLauncherViewModel(GuiStateManager guiStateManager, CommandletService commandletService) {
    super(guiStateManager);
    this.commandletService = commandletService;

    this.isIdeButtonsDisabledProperty.bind(this.guiStateManager.workspaceSelectedProperty().not());
  }

  @Override
  public String getTabTitleKey() {
    return "tab.ide_launcher";
  }

  /**
   * Runs the given IDE commandlet in the currently selected project/workspace context.
   *
   * @param commandlet the commandlet (IDE) to start.
   */
  public void runCommandlet(String commandlet) {
    this.commandletService.runCommandlet(commandlet);
  }

  public BooleanProperty isIdeButtonsDisabledProperty() {
    return isIdeButtonsDisabledProperty;
  }

}
