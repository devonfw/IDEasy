package com.devonfw.ide.gui.service;

import java.nio.file.Path;
import java.util.Objects;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert.AlertType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.context.GuiOutputListener;
import com.devonfw.ide.gui.context.GuiStateManager;
import com.devonfw.ide.gui.context.IdeGuiContext;
import com.devonfw.ide.gui.context.IdeGuiLogListener;
import com.devonfw.ide.gui.ui.controls.console.ConsoleController;
import com.devonfw.ide.gui.ui.modal.IdeDialog;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Service to run a commandlet in the currently selected project/workspace context.
 */
public class CommandletService {

  private static final Logger LOG = LoggerFactory.getLogger(CommandletService.class);

  private final GuiStateManager guiStateManager;
  private final ConsoleController consoleController;

  private IdeGuiLogListener guiLogListener;
  private GuiOutputListener guiOutputListener;

  /**
   * Optional action invoked before a commandlet is launched, e.g. to make the console pane visible. Defaults to a no-op. Part of the hack to make the Console
   * auto-show when an IDE is launched, should be removed when Console is reworked.
   */
  private Runnable preLaunchAction = () -> {
  };

  public CommandletService(GuiStateManager guiStateManager, ConsoleController consoleController) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.consoleController = Objects.requireNonNull(consoleController);
    this.guiLogListener = new IdeGuiLogListener(consoleController);
    this.guiOutputListener = new GuiOutputListener(consoleController);
  }

  /**
   * Sets the action that is run before each commandlet launch. Part of the hack to make the Console auto-show when an IDE is launched, should be removed when
   * Console is reworked.
   *
   * @param preLaunchAction the action to run before launching a commandlet (must not be <code>null</code>).
   */
  public void setPreLaunchAction(Runnable preLaunchAction) {
    this.preLaunchAction = Objects.requireNonNull(preLaunchAction);
  }

  /**
   * Runs the given commandlet in the currently selected project/workspace context.
   *
   * @param commandlet the commandlet to run.
   */
  public void runCommandlet(String commandlet) {

    this.preLaunchAction.run();

    Task<Void> commandletTask = runCommandletTask(commandlet);

    this.guiLogListener = new IdeGuiLogListener(consoleController);
    this.guiOutputListener = new GuiOutputListener(consoleController);

    Thread commandletThread = new Thread(commandletTask);
    commandletThread.setDaemon(true);
    commandletThread.start();
  }

  private Task<Void> runCommandletTask(String commandlet) {

    try (ProgressBarTask task = (ProgressBarTask) this.guiStateManager.getCurrentContext()
        .newProgressBarIndeterminate("Starting " + commandlet)) {
      Task<Void> commandletTask = new Task<>() {
        @Override
        protected Void call() {

          try {
            IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.INFO, guiLogListener);
            Path workspacePath = CommandletService.this.guiStateManager.getIdeRootDir()
                .resolve(CommandletService.this.guiStateManager.getSelectedProject())
                .resolve(IdeContext.FOLDER_WORKSPACES)
                .resolve(CommandletService.this.guiStateManager.getSelectedWorkspace());
            IdeGuiContext context = new IdeGuiContext(startContext, workspacePath, CommandletService.this.guiStateManager.getTaskManager());

            context.setOutputListener(guiOutputListener);

            LOG.info("Running commandlet {}", commandlet);

            context.getCommandletManager().getCommandlet(commandlet).run();

            LOG.info("Commandlet {} completed successfully.", commandlet);
          } catch (Exception e) {
            LOG.error("Failed to run commandlet {}: {}", commandlet, e.getMessage(), e);
            consoleController.appendOutput("[ERROR] Failed to launch " + commandlet + ": " + e.getMessage());
          }
          return null;
        }
      };

      commandletTask.setOnFailed(_ -> Platform.runLater(() -> {
        task.close();
        new IdeDialog(AlertType.ERROR, "Error occurred while launching " + commandlet).showAndWait();
      }));
      commandletTask.setOnSucceeded(_ -> Platform.runLater(task::close));
      return commandletTask;
    }
  }

}
