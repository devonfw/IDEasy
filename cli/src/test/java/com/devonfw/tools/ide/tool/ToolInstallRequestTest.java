package com.devonfw.tools.ide.tool;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.process.ProcessContext;

/**
 * Tests for {@link ToolInstallRequest} parent state inheritance and <code>ignoreProject</code> handling.
 */
class ToolInstallRequestTest extends AbstractIdeContextTest {

  /**
   * Verify that a dependency request inherits <code>ignoreProject</code> and process context from its parent request.
   */
  @Test
  void testParentIgnoreProjectIsInherited() {
    // arrange
    ToolInstallRequest parent = new ToolInstallRequest(false);
    parent.setIgnoreProject(true);
    IdeTestContext context = newContext(PROJECT_BASIC);
    ProcessContext processContext = context.newProcess();
    parent.setProcessContext(processContext);

    // act
    ToolInstallRequest dependencyRequest = new ToolInstallRequest(parent);

    // assert
    assertThat(dependencyRequest.isIgnoreProject()).isTrue();
    assertThat(dependencyRequest.getProcessContext()).isEqualTo(processContext);
  }
}
