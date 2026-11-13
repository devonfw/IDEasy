package com.devonfw.ide.gui.core.service;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.scene.control.Alert.AlertType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.ide.gui.core.context.GuiOutputListener;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.context.IdeGuiContext;
import com.devonfw.ide.gui.core.context.IdeGuiLogListener;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.modal.IdeDialog;
import com.devonfw.ide.gui.core.progress.ProgressBarTask;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Service to run a commandlet in the currently selected project/workspace context.
 */
public class CommandletService {

  private static final Logger LOG = LoggerFactory.getLogger(CommandletService.class);

  private final GuiStateManager guiStateManager;

  private final ObservableSet<String> runningCommandlets;

  private IdeGuiLogListener guiLogListener;
  private GuiOutputListener guiOutputListener;
  private GuiEventBus eventBus;

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
  public CommandletService(GuiStateManager guiStateManager) {
    this.guiStateManager = Objects.requireNonNull(guiStateManager);
    this.runningCommandlets = FXCollections.observableSet(new HashSet<>());
    this.eventBus = guiStateManager.getEventBus();
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
   * Starts a background launch in the selected project/workspace context. Call on the JavaFX application thread. Repeated requests for an IDE that is
   * already starting are ignored.
   *
   * @param commandlet the commandlet to run.
   */
  public void runCommandlet(String commandlet) {

    if (!this.runningCommandlets.add(commandlet)) {
      return;
    }
    try {
      this.preLaunchAction.run();

      Task<Void> commandletTask = runCommandletTask(commandlet);
      commandletTask.stateProperty().addListener((_, _, state) -> {
        if ((state == State.SUCCEEDED) || (state == State.FAILED) || (state == State.CANCELLED)) {
          this.runningCommandlets.remove(commandlet);
        }
      });

      this.guiLogListener = new IdeGuiLogListener(guiStateManager.getEventBus());
      this.guiOutputListener = new GuiOutputListener(guiStateManager.getEventBus());

      Thread commandletThread = new Thread(commandletTask);
      commandletThread.setDaemon(true);
      commandletThread.start();
    } catch (RuntimeException | Error e) {
      this.runningCommandlets.remove(commandlet);
      throw e;
    }
  }

  /**
   * @param commandlet the IDE whose launch state is observed.
   * @return whether a launch of this IDE is already in progress. Access on the JavaFX application thread.
   */
  public BooleanBinding runningBinding(String commandlet) {

    return Bindings.createBooleanBinding(() -> this.runningCommandlets.contains(commandlet), this.runningCommandlets);
  }

  /**
   * Creates the background launch task without starting it.
   *
   * @param commandlet the commandlet to launch.
   * @return the task for the launch.
   */
  protected Task<Void> runCommandletTask(String commandlet) {

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
            guiLogListener.flushAndEndBuffering();
            LOG.error("Failed to run commandlet {}: {}", commandlet, e.getMessage(), e);

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
