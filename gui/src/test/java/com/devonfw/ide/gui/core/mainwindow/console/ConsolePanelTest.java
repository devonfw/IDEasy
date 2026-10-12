package com.devonfw.ide.gui.core.mainwindow.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.ide.gui.HeadlessApplicationTest;
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.service.NlsService;
import com.devonfw.tools.ide.log.IdeLogEntry;
import com.devonfw.tools.ide.log.IdeLogLevel;

class ConsolePanelTest extends HeadlessApplicationTest {

  private ConsoleController consoleController;

  @TempDir
  private Path mockIdeRoot;

  @Override
  public void start(Stage stage) throws IOException {

    URL consoleViewUrl = getClass().getResource("console.fxml");
    assertThat(consoleViewUrl).as("Cannot resolve console UI FXML resource!").isNotNull();

    GuiStateManager guiStateManager = new GuiStateManager(this.mockIdeRoot.toString());
    this.consoleController = guiStateManager.getConsoleController();

    NlsService nlsService = guiStateManager.getNlsService();

    FXMLLoader fxmlLoader = new FXMLLoader(consoleViewUrl);
    fxmlLoader.setResources(nlsService.getResourceBundle());
    fxmlLoader.setControllerFactory(clazz -> clazz == ConsoleController.class ? this.consoleController : null);
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus();
    stage.show();
  }

  /**
   * Tests whether appendOutput() methods actually print to the console.
   */
  @Test
  void testConsoleOutputPrintsCorrectly() {

    Platform.runLater(() -> {
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Hello World!"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Test"));
    });
    waitForFxEvents();

    List<String> snapshot = consoleController.getConsoleOutputSnapshot();
    assertThat(snapshot).anyMatch(s -> s.contains("Hello World!"));
    assertThat(snapshot).anyMatch(s -> s.contains("Test"));
  }

  /**
   * Tests console output with log levels.
   */
  @Test
  void testConsoleOutputWithLogLevels() {

    Platform.runLater(() -> {
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Info message"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.ERROR, "Error message"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.WARNING, "Warning message"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.DEBUG, "Debug message"));
    });
    waitForFxEvents();

    List<String> snapshot = consoleController.getConsoleOutputSnapshot();
    assertThat(snapshot).hasSize(4);
    assertThat(snapshot).anyMatch(s -> s.contains("[INFO] Info message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[ERROR] Error message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[WARNING] Warning message"));
    assertThat(snapshot).anyMatch(s -> s.contains("[DEBUG] Debug message"));
  }

  @Test
  void testConsoleClear() {

    Platform.runLater(() -> {
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Hello World!"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Test"));
    });
    waitForFxEvents();

    Platform.runLater(() -> consoleController.clearConsole());
    waitForFxEvents();

    assertThat(consoleController.getConsoleOutputSnapshot()).isEmpty();
  }

  @Test
  void testLineCountUpdates() {
    Platform.runLater(() -> {
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Line 1"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Line 2"));
      consoleController.appendOutput(new IdeLogEntry(IdeLogLevel.INFO, "Line 3"));
    });
    waitForFxEvents();

    assertThat(consoleController.getConsoleOutputSnapshot()).hasSize(3);
  }

  @Test
  void testAutoScrollCheckboxEnabledByDefault() {
    assertThat(consoleController.isAutoScrollEnabled()).isTrue();
  }
}
