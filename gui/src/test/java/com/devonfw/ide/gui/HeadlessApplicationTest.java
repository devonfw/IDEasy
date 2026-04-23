package com.devonfw.ide.gui;

/**
 * Headless testing is often required for contexts, in which the device running the test does not have access to a physical display (e.g. GitHub CI).
 *
 * @see <a href="https://aqua-cloud.io/de/ui-tests-ein-umfassender-leitfaden/">UI-Tests: Ein umfassender Leitfaden</a>
 * @see <a href="https://testgrid.io/blog/ui-testing/#best-practices-for-ui-testing">Best Practices for UI Testing</a>
 */
public abstract class HeadlessApplicationTest extends IdeGuiMockRootTest {

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
}
