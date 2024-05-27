package com.devonfw.ide.gui;

import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;

import java.io.IOException;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Base Test
 */
public class AppBaseTest extends ApplicationTest {

  @Override
  public void start(Stage stage) throws IOException {

    new App().start(stage);
  }

  /**
   * Set up headless testing
   *
   * @throws IOException
   */
  @BeforeAll
  public static void setupHeadlessMode() {

    if (Boolean.getBoolean("headless")) {
      System.setProperty("testfx.robot", "glass");
      System.setProperty("glass.platform", "Monocle");
      System.setProperty("testfx.headless", "true");
      System.setProperty("prism.order", "sw");
      System.setProperty("java.awt.headless", "true");
    }

  }

  /**
   * Test if welcome message is shown when GUI is started
   */
  @Test
  public void ensureHelloMessageIsShownOnStartUp() {

    verifyThat("#hellomessage", NodeMatchers.isNotNull());
  }

}
