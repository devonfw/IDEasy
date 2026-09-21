package com.devonfw.ide.gui.feature.ide.launcher;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.service.CommandletService;
import com.devonfw.ide.gui.core.tab.TabViewModel;

/**
 * View model of the IDE launcher tab. It exposes the state that drives the launcher buttons, which stay disabled until a project/workspace is selected.
 */
public class IdeLauncherViewModel extends TabViewModel {

  private final CommandletService commandletService;

  private final BooleanProperty isIdeButtonsDisabledProperty = new SimpleBooleanProperty();

  /**
   * Creates the view model.
   *
   * @param guiStateManager the app-wide selection and context holder.
   * @param commandletService the service that launches the IDEs.
   */
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
