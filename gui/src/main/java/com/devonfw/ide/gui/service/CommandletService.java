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
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Service to run a commandlet in the currently selected project/workspace context. It runs the commandlet on a background thread within the current
 * {@link GuiStateManager} context, tracks progress via a progress bar, routes the commandlet's output to the shared {@link ConsoleController}, and surfaces
 * failures. Reusable by any tab that needs to start a commandlet.
 */
public class CommandletService {

  private static final Logger LOG = LoggerFactory.getLogger(CommandletService.class);

  private final GuiStateManager guiStateManager;
  private final ConsoleController consoleController;

  private final IdeGuiLogListener guiLogListener;
  private final GuiOutputListener guiOutputListener;

  /**
   * Optional action invoked before a commandlet is launched, e.g. to make the console pane visible. Defaults to a no-op.
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
   * Sets the action that is run before each commandlet launch.
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
                .resolve("workspaces")
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
