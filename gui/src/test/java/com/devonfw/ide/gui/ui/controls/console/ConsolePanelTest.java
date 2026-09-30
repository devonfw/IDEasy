<<<<<<<< HEAD:gui/src/test/java/com/devonfw/ide/gui/ui/controls/console/ConsolePanelTest.java
package com.devonfw.ide.gui.ui.controls.console;
========
package com.devonfw.ide.gui.core.mainwindow.console;
>>>>>>>> main:gui/src/test/java/com/devonfw/ide/gui/core/mainwindow/console/ConsolePanelTest.java

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
<<<<<<<< HEAD:gui/src/test/java/com/devonfw/ide/gui/ui/controls/console/ConsolePanelTest.java
import com.devonfw.ide.gui.event.GuiEventBus;
import com.devonfw.ide.gui.service.NlsService;
========
import com.devonfw.ide.gui.core.context.GuiStateManager;
import com.devonfw.ide.gui.core.service.NlsService;
>>>>>>>> main:gui/src/test/java/com/devonfw/ide/gui/core/mainwindow/console/ConsolePanelTest.java
import com.devonfw.tools.ide.log.IdeLogLevel;

class ConsolePanelTest extends HeadlessApplicationTest {

  private ConsoleViewModel consoleViewModel;
  private ConsoleView consoleView;

  @TempDir
  private Path mockIdeRoot;

  @Override
  public void start(Stage stage) throws IOException {

    URL consoleViewUrl = getClass().getResource("console.fxml");
    assertThat(consoleViewUrl).as("Cannot resolve console UI FXML resource!").isNotNull();

<<<<<<<< HEAD:gui/src/test/java/com/devonfw/ide/gui/ui/controls/console/ConsolePanelTest.java
    NlsService nlsService = new NlsService(Locale.ENGLISH);
    GuiEventBus eventBus = new GuiEventBus();

    FXMLLoader fxmlLoader = new FXMLLoader(consoleViewUrl);
    fxmlLoader.setResources(nlsService.getResourceBundle());
    fxmlLoader.setControllerFactory(clazz -> {
      if (clazz == ConsoleView.class) {
        consoleViewModel = new ConsoleViewModel(eventBus);
        return new ConsoleView(consoleViewModel, nlsService);
      }
      return null;
    });
========
    GuiStateManager guiStateManager = new GuiStateManager(this.mockIdeRoot.toString());
    this.consoleController = guiStateManager.getConsoleController();

    NlsService nlsService = guiStateManager.getNlsService();

    FXMLLoader fxmlLoader = new FXMLLoader(consoleViewUrl);
    fxmlLoader.setResources(nlsService.getResourceBundle());
    fxmlLoader.setControllerFactory(clazz -> clazz == ConsoleController.class ? this.consoleController : null);
>>>>>>>> main:gui/src/test/java/com/devonfw/ide/gui/core/mainwindow/console/ConsolePanelTest.java
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); // sometimes needed for headless setup to work
    stage.show();
<<<<<<<< HEAD:gui/src/test/java/com/devonfw/ide/gui/ui/controls/console/ConsolePanelTest.java

    consoleView = fxmlLoader.getController();
========
>>>>>>>> main:gui/src/test/java/com/devonfw/ide/gui/core/mainwindow/console/ConsolePanelTest.java
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
}
