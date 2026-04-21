package com.devonfw.ide.gui;

import java.nio.file.Path;

import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Headless testing is often required for contexts, in which the device running the test does not have access to a physical display (e.g. GitHub CI).
 *
 * @see <a href="https://aqua-cloud.io/de/ui-tests-ein-umfassender-leitfaden/">UI-Tests: Ein umfassender Leitfaden</a>
 * @see <a href="https://testgrid.io/blog/ui-testing/#best-practices-for-ui-testing">Best Practices for UI Testing</a>
 */
public abstract class IdeGuiApplicationTest extends ApplicationTest {

  @TempDir(cleanup = CleanupMode.ON_SUCCESS)
  private static Path mockIdeRoot;

  //setting up for headless testing
  static {

    System.setProperty("testfx.robot", "glass");
    System.setProperty("testfx.headless", "true");
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
    System.setProperty("java.awt.headless", "true");
    System.setProperty("glass.platform", "Monocle");
    System.setProperty("monocle.platform", "Headless");
    System.setProperty("testfx.setup.timeout", "10000"); // increased timeout for testing on server-side CIs
  }

  /**
   * @return a mock {@link Path} to a temporary IDE_ROOT directory.
   */
  public static Path getMockIdeRoot() {

    return mockIdeRoot;
  }
}
