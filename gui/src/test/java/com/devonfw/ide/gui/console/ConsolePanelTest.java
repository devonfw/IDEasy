package com.devonfw.ide.gui.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

import java.io.IOException;
import java.net.URL;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import com.devonfw.ide.gui.HeadlessApplicationTest;

class ConsolePanelTest extends HeadlessApplicationTest {

  private ConsoleController consoleController;

  @Override
  public void start(Stage stage) throws IOException {

    URL consoleViewUrl = getClass().getResource("console.fxml");
    assertThat(consoleViewUrl).as("Cannot resolve console UI FXML resource!").isNotNull();

    FXMLLoader fxmlLoader = new FXMLLoader(consoleViewUrl);
    Parent root = fxmlLoader.load();
    stage.setScene(new Scene(root));
    stage.requestFocus(); //sometimes needed for headless setup to work
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

    // Verify that the output is displayed in the TextArea
    assertThat(consoleController.getConsoleOutputSnapshot()).containsExactlyInAnyOrder("Hello World!", "Test");
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

}
