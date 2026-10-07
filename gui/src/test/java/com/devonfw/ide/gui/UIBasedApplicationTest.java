package com.devonfw.ide.gui;

import org.testfx.framework.junit5.ApplicationTest;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.AbstractIdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test class for performing UI-related tests. This test class creates a "headless" testing environment, which is often required for contexts, in which the
 * device running the test does not have access to an actual physical display (GitHub CI).
 *
 * @see <a href="https://aqua-cloud.io/de/ui-tests-ein-umfassender-leitfaden/">UI-Tests: Ein umfassender Leitfaden</a>
 * @see <a href="https://testgrid.io/blog/ui-testing/#best-practices-for-ui-testing">Best Practices for UI Testing</a>
 */
public class UIBasedApplicationTest extends ApplicationTest {

  private IdeTestContext testContext;

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
   * Set the test context for this UI-based test.
   *
   * @param projectFolderName name of the project-folder under /ide-projects
   * @param projectName name of the project to set the context up in, as a subfolder of the projectFolderName.
   * @see IdeTestContext
   */
  public void setTestContext(String projectFolderName, String projectName) {

    testContext = AbstractIdeContextTest.newContext(projectFolderName, projectName);
  }

  /**
   * @return the currently used {@link AbstractIdeTestContext test context}
   * @see IdeTestContext
   */
  public AbstractIdeTestContext getTestContext() {

    return testContext;
  }
}
