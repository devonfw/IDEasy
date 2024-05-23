package com.devonfw.ide.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base Test
 */
public class AppBaseTest extends ApplicationTest {

  Pane mainRoot;

  Stage mainStage;

  public static File userHome = null;

  /**
   * Set up headless testing
   *
   * @throws IOException
   */
  @Before
  public void setupHeadlessMode() throws IOException {

    if (Boolean.getBoolean("headless")) {
      System.setProperty("testfx.robot", "glass");
      System.setProperty("testfx.headless", "true");
      System.setProperty("prism.order", "sw");
      System.setProperty("prism.text", "t2k");
      System.setProperty("java.awt.headless", "true");
    }

  }

  /**
   * Start the GUI and set everything up
   */
  @Override
  public void start(Stage stage) throws Exception {

    this.mainStage = stage;
    FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
    this.mainRoot = loader.load();
    stage.setScene(new Scene(this.mainRoot));
    stage.show();
    stage.toFront();
  }

  /**
   * @throws TimeoutException
   */
  @After
  public void afterEachTest() throws TimeoutException {

    FxToolkit.hideStage();
    release(new KeyCode[] {});
    release(new MouseButton[] {});
  }

  /**
   * Helper method to retrieve Java FX GUI components
   *
   * @param <T>
   * @param query
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T extends Node> T find(final String query) {

    // temporary 'fix' because lookup(#id)... throws error
    return (T) this.mainStage.getScene().lookup(query);

  }

  /**
   * Test if welcome message is shown when GUI is started
   */
  @Test
  public void ensureHelloMessageIsShownOnStartUp() {

    assertThat((Parent) find("#hellomessage")).isNotNull();
  }
}
