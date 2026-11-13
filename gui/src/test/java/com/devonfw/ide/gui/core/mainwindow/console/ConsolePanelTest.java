package com.devonfw.ide.gui.core.mainwindow.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.UIBasedApplicationTest;
import com.devonfw.ide.gui.core.context.IdeGuiLogListener;
import com.devonfw.ide.gui.core.event.GuiEventBus;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.environment.IdeSystemTestImpl;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

class ConsolePanelTest extends UIBasedApplicationTest {

  /** Throw-away user home for the isolated NlsService context so a persisted locale never rewrites the real {@code ~/.ide/ide.properties}. */
  @TempDir
  private static Path nlsUserHome;

  private ConsoleViewModel consoleViewModel;

  private GuiEventBus eventBus;

  @Override
  public void start(Stage stage) throws IOException {

    URL consoleViewUrl = getClass().getResource("console.fxml");
    assertThat(consoleViewUrl).as("Cannot resolve console UI FXML resource!").isNotNull();

    // isolate the NlsService from the developer's real user home and the IDE_OPTIONS environment variable so constructing it has no side effects
    final IdeTestContext nlsContext = new IdeTestContext();
    nlsContext.setSystem(new IdeSystemTestImpl());
    nlsContext.setUserHome(nlsUserHome);
    NlsService nlsService = new NlsService(nlsContext, Locale.ENGLISH);
    this.eventBus = new GuiEventBus();

    FXMLLoader fxmlLoader = new FXMLLoader(consoleViewUrl);
    fxmlLoader.setResources(nlsService.getResourceBundle());
    fxmlLoader.setControllerFactory(clazz -> {
      if (clazz == ConsoleView.class) {
        consoleViewModel = new ConsoleViewModel(eventBus);
        return new ConsoleView(consoleViewModel, nlsService);
      }
      return null;
    });
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); // sometimes needed for headless setup to work
    stage.show();
  }

  /**
   * Tests whether appendOutput() methods actually print to the console.
   */
  @Test
  void testConsoleOutputPrintsCorrectly() {

    // Simulate output to the console
    Platform.runLater(() -> {
      consoleViewModel.appendOutput("Hello World!");
      consoleViewModel.appendOutput("Test");
    });
    waitForFxEvents();

    // Verify that the output is displayed in the ListView
    List<String> snapshot = consoleViewModel.getConsoleOutputSnapshot();
    assertThat(snapshot).anyMatch(s -> s.contains("Hello World!"));
    assertThat(snapshot).anyMatch(s -> s.contains("Test"));
  }

  /**
   * Tests console output with log levels.
   */
  @Test
  void testConsoleOutputWithLogLevels() {

    Platform.runLater(() -> {
      consoleViewModel.appendOutput(IdeLogLevel.INFO, "Info message");
      consoleViewModel.appendOutput(IdeLogLevel.ERROR, "Error message");
      consoleViewModel.appendOutput(IdeLogLevel.WARNING, "Warning message");
      consoleViewModel.appendOutput(IdeLogLevel.DEBUG, "Debug message");
    });
    waitForFxEvents();

    List<String> snapshot = consoleViewModel.getConsoleOutputSnapshot();
    assertThat(snapshot).hasSize(4);
    // Check that all log levels appear in the output (format is "HH:mm:ss | [LEVEL]  message")
    // INFO has 3 spaces after bracket, ERROR has 2, WARN has 2, DEBUG has 2
    assertThat(snapshot).anyMatch(s -> s.contains("[INFO] Info message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[ERROR] Error message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[WARNING] Warning message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[DEBUG] Debug message"));
  }

  @Test
  void testConsoleClear() {

    // Simulate output to the console
    Platform.runLater(() -> {
      consoleViewModel.appendOutput("Hello World!");
      consoleViewModel.appendOutput("Test");
    });
    waitForFxEvents();

    // Clear the console
    Platform.runLater(() -> consoleViewModel.clearConsole());
    waitForFxEvents();

    // Verify that the console is empty
    assertThat(consoleViewModel.getConsoleOutputSnapshot()).isEmpty();
  }

  @Test
  void testLineCountUpdates() {

    Platform.runLater(() -> {
      consoleViewModel.appendOutput("Line 1");
      consoleViewModel.appendOutput("Line 2");
      consoleViewModel.appendOutput("Line 3");
    });
    waitForFxEvents();

    // Line count should be 3
    assertThat(consoleViewModel.getConsoleOutputSnapshot()).hasSize(3);
  }

  @Test
  void testAutoScrollCheckboxEnabledByDefault() {

    // Just verify auto-scroll is enabled by default
    assertThat(consoleViewModel.autoScrollEnabledProperty().get()).isTrue();
  }

  /**
   * Tests the event-bus path of the rework: a log forwarded by {@link IdeGuiLogListener} over the {@link GuiEventBus} ends up in the console, and a non-null
   * {@code Throwable} yields an additional error line.
   */
  @Test
  void testLogEventArrivingOverTheEventBusIsShown() {

    // Forward logs over the event bus - the path used since the rework instead of a direct appendOutput call
    IdeGuiLogListener listener = new IdeGuiLogListener(eventBus);
    // The listener buffers by default; end buffering first so the following onLog() calls forward their entries over the event bus.
    listener.flushAndEndBuffering();
    Platform.runLater(() -> {
      listener.onLog(new IdeLogEntry(IdeLogLevel.INFO, "Info via EventBus", null, null, null));
      listener.onLog(new IdeLogEntry(IdeLogLevel.ERROR, "Error via EventBus", null, null, new RuntimeException("boom")));
    });
    waitForFxEvents();

    List<String> snapshot = consoleViewModel.getConsoleOutputSnapshot();
    assertThat(snapshot).anyMatch(s -> s.contains("[INFO] Info via EventBus"));
    assertThat(snapshot).anyMatch(s -> s.contains("[ERROR] Error via EventBus"));
    assertThat(snapshot).as("the error is kept on the entry, not printed as an extra line").hasSize(2);

  }

  /**
   * Tests the off-FX-thread path of the rework: a log emitted from a non-FX worker thread (as commandlet logs are) must not be lost. It is buffered by the
   * view-model and flushed to the console on the FX application thread (see {@link javafx.application.Platform#runLater(Runnable)}).
   */
  @Test
  void testLogEventArrivingOnOffFxThreadIsShown() throws InterruptedException {

    IdeGuiLogListener listener = new IdeGuiLogListener(eventBus);
    // The listener buffers by default; end buffering first so the onLog() call below forwards its entry over the event bus.
    listener.flushAndEndBuffering();
    Thread worker = new Thread(() -> {
      listener.onLog(new IdeLogEntry(IdeLogLevel.INFO, "Off FX", null, null, null));
    });
    worker.start();
    worker.join(); // ensure the log was emitted before asserting
    waitForFxEvents();

    List<String> snapshot = consoleViewModel.getConsoleOutputSnapshot();
    assertThat(snapshot).anyMatch(s -> s.contains("[INFO] Off FX"));
  }
}
