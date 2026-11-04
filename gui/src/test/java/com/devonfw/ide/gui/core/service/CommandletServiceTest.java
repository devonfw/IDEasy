package com.devonfw.ide.gui.core.service;

import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.stage.Stage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.IdeSystemTestImpl;

/**
 * Tests launch coordination with controlled tasks, without installing or starting real IDEs.
 */
public class CommandletServiceTest extends UIBasedApplicationTest {

  @TempDir
  Path ideRoot;

  private CommandletService service;

  private List<Task<Void>> tasks;

  private CompletableFuture<Void> completion;

  @Override
  public void start(Stage stage) {

    this.tasks = new ArrayList<>();
    this.completion = new CompletableFuture<>();
    IdeTestContext context = new IdeTestContext();
    context.setSystem(new IdeSystemTestImpl());
    context.setUserHome(this.ideRoot);
    GuiStateManager stateManager = new GuiStateManager(this.ideRoot.toString(), context);
    this.service = new CommandletService(stateManager) {
      @Override
      protected Task<Void> runCommandletTask(String commandlet) {

        Task<Void> task = new Task<>() {
          @Override
          protected Void call() throws Exception {

            return CommandletServiceTest.this.completion.get(10, TimeUnit.SECONDS);
          }
        };
        CommandletServiceTest.this.tasks.add(task);
        return task;
      }
    };
  }

  @AfterEach
  void finishTasks() {

    interact(() -> this.tasks.forEach(task -> task.cancel(true)));
  }

  @Test
  void testRepeatedClickStartsOnlyOneTask() {

    interact(() -> {
      this.service.runCommandlet("intellij");
      this.service.runCommandlet("intellij");

      Assertions.assertThat(this.tasks).hasSize(1);
    });
  }

  @Test
  void testOnlyTheRequestedIdeIsBusyAndOtherIdesCanStart() {

    interact(() -> {
      BooleanBinding intellijRunning = this.service.runningBinding("intellij");
      BooleanBinding eclipseRunning = this.service.runningBinding("eclipse");
      Assertions.assertThat(intellijRunning.get()).isFalse();
      Assertions.assertThat(eclipseRunning.get()).isFalse();

      this.service.runCommandlet("intellij");

      Assertions.assertThat(intellijRunning.get()).isTrue();
      Assertions.assertThat(eclipseRunning.get()).isFalse();
      Assertions.assertThat(this.service.runningBinding("vscode").get()).isFalse();
      Assertions.assertThat(this.service.runningBinding("android-studio").get()).isFalse();

      this.service.runCommandlet("eclipse");

      Assertions.assertThat(this.tasks).hasSize(2);
      Assertions.assertThat(eclipseRunning.get()).isTrue();
    });
  }

  @ParameterizedTest
  @EnumSource(value = State.class, names = { "SUCCEEDED", "FAILED", "CANCELLED" })
  void testIdeCanBeLaunchedAgainAfterTaskEnds(State state) throws Exception {

    interact(() -> this.service.runCommandlet("intellij"));
    Task<Void> task = this.tasks.getFirst();

    switch (state) {
      case SUCCEEDED -> {
        this.completion.complete(null);
        task.get(5, TimeUnit.SECONDS);
      }
      case FAILED -> {
        this.completion.completeExceptionally(new IllegalStateException("Expected launch failure"));
        Assertions.assertThatExceptionOfType(ExecutionException.class).isThrownBy(() -> task.get(5, TimeUnit.SECONDS));
      }
      case CANCELLED -> interact(() -> task.cancel(true));
      default -> throw new IllegalArgumentException("Unexpected test state: " + state);
    }
    waitForFxEvents();

    interact(() -> {
      Assertions.assertThat(task.getState()).isEqualTo(state);
      Assertions.assertThat(this.service.runningBinding("intellij").get()).isFalse();

      this.service.runCommandlet("intellij");
      Assertions.assertThat(this.tasks).hasSize(2);
    });
  }

  @Test
  void testIdeIsNotLeftBusyIfLaunchSetupFails() {

    interact(() -> {
      this.service.setPreLaunchAction(() -> {
        throw new IllegalStateException("Expected setup failure");
      });

      Assertions.assertThatIllegalStateException().isThrownBy(() -> this.service.runCommandlet("intellij"));
      Assertions.assertThat(this.service.runningBinding("intellij").get()).isFalse();
    });
  }
}
