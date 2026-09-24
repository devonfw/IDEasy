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
import com.devonfw.ide.gui.event.GuiEventBus;
import com.devonfw.ide.gui.event.console.LogEvent;
import com.devonfw.ide.gui.ui.modal.IdeDialog;
import com.devonfw.ide.gui.ui.progress.ProgressBarTask;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Service to run a commandlet in the currently selected project/workspace context.
 */
public class CommandletService {

  private static final Logger LOG = LoggerFactory.getLogger(CommandletService.class);

  private final GuiStateManager guiStateManager;
  private final GuiEventBus eventBus;

  private IdeGuiLogListener guiLogListener;
  private GuiOutputListener guiOutputListener;

  /**
   * Optional action invoked before a commandlet is launched, e.g. to make the console pane visible. Defaults to a no-op. Part of the hack to make the Console
   * auto-show when an IDE is launched, should be removed when Console is reworked.
   */
  private Runnable preLaunchAction = () -> {
  };

  /**
   * Creates the service.
   *
   * @param guiStateManager the app-wide selection and context holder.
   */
  public CommandletService(GuiStateManager guiStateManager, GuiEventBus eventBus) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.eventBus = Objects.requireNonNull(eventBus);
    this.guiLogListener = new IdeGuiLogListener(eventBus);
    this.guiOutputListener = new GuiOutputListener(eventBus);
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

    this.guiLogListener = new IdeGuiLogListener(eventBus);
    this.guiOutputListener = new GuiOutputListener(eventBus);

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
            eventBus.sendEvent(new LogEvent(new IdeLogEntry(IdeLogLevel.ERROR, "[GUI ERROR] Failed to launch " + commandlet + ": " + e.getMessage())));
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
