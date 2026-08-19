package com.devonfw.ide.gui.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.progress.GuiTask;
import com.devonfw.ide.gui.progress.TaskState;
import com.devonfw.ide.gui.progress.TaskStats;
import com.devonfw.ide.gui.progress.step.GuiStep;
import com.devonfw.tools.ide.context.IdeStartContextImpl;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeLogListenerBuffer;
import com.devonfw.tools.ide.step.Step;

/**
 * Tests that {@link IdeGuiContext} maintains the {@link Step} stack exactly like {@link com.devonfw.tools.ide.context.AbstractIdeContext} does.
 * <p>
 * The GUI keeps its own stack because the inherited one is private, so these tests pin the contract that the overridden
 * {@link IdeGuiContext#newStep(boolean, String, Object...)}, {@link IdeGuiContext#getCurrentStep()} and {@link IdeGuiContext#endStep} must honour. We extend
 * {@link HeadlessApplicationTest} because registering a task with the {@link TaskManager} happens on the JavaFX Application Thread.
 */
class IdeGuiContextStepTest extends HeadlessApplicationTest {

  @TempDir
  private static Path workingDirectory;

  private TaskManager taskManager;

  private IdeGuiContext context;

  @BeforeEach
  void setUp() {

    this.taskManager = new TaskManager();
    IdeStartContextImpl startContext = new IdeStartContextImpl(IdeLogLevel.DEBUG, new IdeLogListenerBuffer());
    this.context = new IdeGuiContext(startContext, workingDirectory, this.taskManager);
    waitForFxEvents();
  }

  @Test
  void shouldTrackCurrentStepAcrossNesting() {

    assertThat(this.context.getCurrentStep()).as("no step is running initially").isNull();

    Step root = this.context.newStep("root");
    assertThat(this.context.getCurrentStep()).isSameAs(root);

    Step child = this.context.newStep("child");
    assertThat(this.context.getCurrentStep()).isSameAs(child);
    assertThat(child.getParent()).as("nested step must know its parent").isSameAs(root);

    child.success();
    child.close();
    assertThat(this.context.getCurrentStep()).as("ending a nested step must pop back to its parent").isSameAs(root);

    root.success();
    root.close();
    assertThat(this.context.getCurrentStep()).as("ending the root step must leave no running step").isNull();
  }

  @Test
  void shouldPopStepThatEndedWithError() {

    Step root = this.context.newStep("root");
    Step child = this.context.newStep("child");

    child.error("boom");
    child.close();
    waitForFxEvents();

    assertThat(this.context.getCurrentStep()).isSameAs(root);
    assertThat(child.isFailure()).isTrue();
    assertThat(((GuiTask) child).getState()).isEqualTo(TaskState.FAILED);
  }

  @Test
  void shouldPopStepThatWasOnlyClosed() {

    Step root = this.context.newStep("root");
    Step child = this.context.newStep("child");

    // a step that is closed without an explicit outcome is recorded as a failure by StepImpl - the GUI must report that honestly.
    child.close();
    waitForFxEvents();

    assertThat(this.context.getCurrentStep()).isSameAs(root);
    assertThat(((GuiTask) child).getState()).isEqualTo(TaskState.FAILED);
  }

  @Test
  void shouldRegisterOnlyRootStepsAsTasks() {

    GuiStep root = (GuiStep) this.context.newStep("root");
    GuiStep child = (GuiStep) this.context.newStep("child");
    waitForFxEvents();

    assertThat(this.taskManager.getTasks()).as("only the root step becomes a task").containsExactly(root);
    assertThat(child.getRoot()).isSameAs(root);
  }

  @Test
  void shouldKeepFinishedStepUntilDismissed() {

    GuiStep root = (GuiStep) this.context.newStep("root");
    waitForFxEvents();

    root.success();
    root.close();
    waitForFxEvents();

    assertThat(this.taskManager.getTasks()).as("a finished step stays until the user dismisses it").containsExactly(root);
    assertThat(root.getState()).isEqualTo(TaskState.SUCCESS);
    assertThat(root.isDismissable()).isTrue();

    this.taskManager.removeTask(root);
    waitForFxEvents();

    assertThat(this.taskManager.getTasks()).isEmpty();
  }

  @Test
  void shouldReportSubStepResultsOnRoot() {

    GuiStep root = (GuiStep) this.context.newStep("root");

    Step succeeding = this.context.newStep("succeeding");
    waitForFxEvents();
    assertThat(root.statsProperty().get()).as("a running sub-step is counted while it runs").isEqualTo(new TaskStats(1, 0, 0));
    succeeding.success();
    succeeding.close();

    Step failing = this.context.newStep("failing");
    failing.error("boom");
    failing.close();

    waitForFxEvents();
    assertThat(root.statsProperty().get()).isEqualTo(new TaskStats(0, 1, 1));

    root.success();
    root.close();
    waitForFxEvents();

    assertThat(root.statsProperty().get()).as("the finished root keeps the tally of its sub-steps").isEqualTo(new TaskStats(0, 1, 1));
    assertThat(root.statsProperty().get().total()).isEqualTo(2);
    assertThat(root.getState()).isEqualTo(TaskState.SUCCESS);
  }

  /**
   * The subtitle names the innermost running sub-step, so the user sees what the task is doing right now.
   */
  @Test
  void shouldNameInnermostRunningSubStepAsSubtitle() {

    GuiStep root = (GuiStep) this.context.newStep("root");
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).as("a root step without sub-steps has nothing to name").isEmpty();

    Step child = this.context.newStep("child");
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).isEqualTo("child");

    Step grandChild = this.context.newStep("grand-child");
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).as("the innermost step wins").isEqualTo("grand-child");

    grandChild.success();
    grandChild.close();
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).as("ending the innermost step falls back to its parent").isEqualTo("child");

    child.success();
    child.close();
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).as("back at the root there is no sub-step to name").isEmpty();

    Step second = this.context.newStep("second");
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).isEqualTo("second");
    second.success();
    second.close();

    root.success();
    root.close();
    waitForFxEvents();
    assertThat(root.subtitleProperty().get()).as("a finished task has nothing running").isEmpty();
  }

  /**
   * The expanded task shows one flat list, so a grandchild has to land on the root rather than nesting under its own parent - and the order has to be the
   * order in which the steps started.
   */
  @Test
  void shouldFlattenAllDescendantsOntoTheRootInStartOrder() {

    GuiStep root = (GuiStep) this.context.newStep("root");
    GuiStep first = (GuiStep) this.context.newStep("first");
    GuiStep grandChild = (GuiStep) this.context.newStep("grand-child");
    grandChild.success();
    grandChild.close();
    first.success();
    first.close();
    GuiStep second = (GuiStep) this.context.newStep("second");
    waitForFxEvents();

    assertThat(root.getSubTasks()).as("every descendant is a direct entry of the root, in start order")
        .containsExactly(first, grandChild, second);
    assertThat(first.getSubTasks()).as("only the root collects sub-steps").isEmpty();
    assertThat(grandChild.getState()).isEqualTo(TaskState.SUCCESS);
    assertThat(second.getState()).as("a step that is still running keeps the spinner").isEqualTo(TaskState.RUNNING);
  }

  @Test
  void shouldReportAllSubStepsSucceeded() {

    GuiStep root = (GuiStep) this.context.newStep("root");
    for (int i = 0; i < 3; i++) {
      Step child = this.context.newStep("child-" + i);
      child.success();
      child.close();
    }
    root.success();
    root.close();
    waitForFxEvents();

    assertThat(root.statsProperty().get()).isEqualTo(new TaskStats(0, 3, 0));
  }

  /**
   * A task without sub-steps must not render any chips, so its tally stays empty.
   */
  @Test
  void shouldReportNoStatsWithoutSubSteps() {

    GuiStep root = (GuiStep) this.context.newStep("root");
    root.success();
    root.close();
    waitForFxEvents();

    assertThat(root.statsProperty().get()).isEqualTo(TaskStats.NONE);
    assertThat(root.statsProperty().get().isEmpty()).isTrue();
    assertThat(root.displayTextProperty().get()).as("the title carries no tally anymore - the chips do").isEqualTo("root");
  }
}
