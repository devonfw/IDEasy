package com.devonfw.ide.gui.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.nls.NlsService;
import com.devonfw.tools.ide.log.IdeLogLevel;

class ConsolePanelTest extends HeadlessApplicationTest {

  private ConsoleController consoleController;

  @Override
  public void start(Stage stage) throws IOException {

    URL consoleViewUrl = getClass().getResource("console.fxml");
    assertThat(consoleViewUrl).as("Cannot resolve console UI FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(consoleViewUrl);
    fxmlLoader.setResources(new NlsService(Locale.ENGLISH).getResourceBundle());
    fxmlLoader.setController(new ConsoleController(new NlsService(Locale.ENGLISH)));
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); // sometimes needed for headless setup to work
    stage.show();

    consoleController = fxmlLoader.getController();
  }

  /**
   * Tests whether appendOutput() methods actually print to the console.
   */
  @Test
  void testConsoleOutputPrintsCorrectly() {

    // Simulate output to the console
    Platform.runLater(() -> {
      consoleController.appendOutput("Hello World!");
      consoleController.appendOutput("Test");
    });
    waitForFxEvents();

    // Verify that the output is displayed in the ListView
    assertThat(consoleController.getConsoleOutputSnapshot()).containsExactlyInAnyOrder("Hello World!", "Test");
  }

  /**
   * Tests console output with log levels.
   */
  @Test
  void testConsoleOutputWithLogLevels() {

    Platform.runLater(() -> {
      consoleController.appendOutput(IdeLogLevel.INFO, "Info message");
      consoleController.appendOutput(IdeLogLevel.ERROR, "Error message");
      consoleController.appendOutput(IdeLogLevel.WARNING, "Warning message");
      consoleController.appendOutput(IdeLogLevel.DEBUG, "Debug message");
    });
    waitForFxEvents();

    var snapshot = consoleController.getConsoleOutputSnapshot();
    assertThat(snapshot).hasSize(4);
    // Check that all log levels appear in the output (format is "HH:mm:ss | [LEVEL]  message" with proper spacing)
    // INFO has 3 spaces after bracket, ERROR has 2, WARN has 2, DEBUG has 2
    assertThat(snapshot).anyMatch(s -> s.contains("[INFO]   Info message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[ERROR]  Error message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[WARN]   Warning message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[DEBUG]  Debug message"));
  }

  /**
   * Tests console output with error.
   */
  @Test
  void testConsoleOutputWithError() {

    Platform.runLater(() -> consoleController.appendOutput(IdeLogLevel.ERROR, "Error message", new RuntimeException("Test error")));
    waitForFxEvents();

    var snapshot = consoleController.getConsoleOutputSnapshot();
    assertThat(snapshot).hasSize(2);
    // Check that both the error message and the error detail appear
    // Format: "HH:mm:ss | [LEVEL]  message"
    assertThat(snapshot).anyMatch(s -> s.contains("[ERROR]  Error message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[ERROR]    Error: Test error"));
  }

  @Test
  void testConsoleClear() {

    // Simulate output to the console
    Platform.runLater(() -> {
      consoleController.appendOutput("Hello World!");
      consoleController.appendOutput("Test");
    });
    waitForFxEvents();

    // Clear the console
    Platform.runLater(() -> consoleController.clearConsole());
    waitForFxEvents();

    // Verify that the console is empty
    assertThat(consoleController.getConsoleOutputSnapshot()).isEmpty();
  }

  @Test
  void testLineCountUpdates() {
    Platform.runLater(() -> {
      consoleController.appendOutput("Line 1");
      consoleController.appendOutput("Line 2");
      consoleController.appendOutput("Line 3");
    });
    waitForFxEvents();

    // Line count should be 3
    assertThat(consoleController.getConsoleOutputSnapshot()).hasSize(3);
  }

  @Test
  void testAutoScrollCheckboxEnabledByDefault() {
    // Just verify auto-scroll is enabled by default
    assertThat(consoleController.isAutoScrollEnabled()).isTrue();
  }
}
