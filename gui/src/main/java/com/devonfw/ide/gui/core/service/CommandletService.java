package com.devonfw.ide.gui.core.service;

import java.nio.file.Path;
import java.util.Objects;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert.AlertType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.context.GuiOutputListener;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.context.IdeGuiContext;
import com.devonfw.ide.gui.core.context.IdeGuiLogListener;
import com.devonfw.ide.gui.core.event.CommandletLaunchEvent;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.event.console.LogEvent;
import com.devonfw.ide.gui.core.modal.IdeDialog;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Service to run a commandlet in the currently selected project/workspace context.
 */
public class CommandletService {

  private static final Logger LOG = LoggerFactory.getLogger(CommandletService.class);

  private final GuiStateManager guiStateManager;

  private IdeGuiLogListener guiLogListener;
  private GuiOutputListener guiOutputListener;
  private final GuiEventBus eventBus;


  /**
   * Creates the service.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public CommandletService(GuiStateManager guiStateManager) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.eventBus = guiStateManager.getEventBus();
    this.guiLogListener = new IdeGuiLogListener(eventBus);
    this.guiOutputListener = new GuiOutputListener(eventBus);
  }


  /**
   * Runs the given commandlet in the currently selected project/workspace context.
   *
   * @param commandlet the commandlet to run.
   */
  public void runCommandlet(String commandlet) {

    this.eventBus.sendEvent(new CommandletLaunchEvent());

    Task<Void> commandletTask = runCommandletTask(commandlet);

    this.guiLogListener = new IdeGuiLogListener(this.eventBus);
    this.guiOutputListener = new GuiOutputListener(this.eventBus);

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
            Path workspacePath = guiStateManager.getCurrentContext().getWorkspacePath();
            IdeGuiContext context = new IdeGuiContext(startContext, workspacePath, CommandletService.this.guiStateManager.getTaskManager());

            context.setOutputListener(guiOutputListener);

            LOG.info("Running commandlet {}", commandlet);

            context.getCommandletManager().getCommandlet(commandlet).run();

            LOG.info("Commandlet {} completed successfully.", commandlet);
          } catch (Exception e) {
            LOG.error("Failed to run commandlet {}: {}", commandlet, e.getMessage(), e);

            eventBus.sendEvent(new LogEvent(
                new IdeLogEntry(IdeLogLevel.ERROR, "[ERROR] Failed to launch " + commandlet + ": " + e.getMessage())
            ));
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
