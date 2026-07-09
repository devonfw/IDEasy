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

  /**
   * Verify that creating a dependency request with a null parent does not crash and defaults ignoreProject to false.
   */
  @Test
  void testDependencyRequestWithNullParent() {
    // arrange & act
    ToolInstallRequest dependencyRequest = new ToolInstallRequest(null);

    // assert
    assertThat(dependencyRequest.isIgnoreProject()).isFalse();
    assertThat(dependencyRequest.getProcessContext()).isNull();
  }

  /**
   * Verify that {@link ToolInstallRequest#isIgnoreProject() ignoreProject} can be changed after creating a request.
   */
  @Test
  @SuppressWarnings("ConstantConditions")
  void testIgnoreProjectCanBeChanged() {
    // arrange
    ToolInstallRequest request = new ToolInstallRequest(false);
    assertThat(request.isIgnoreProject()).isFalse();

    // act & assert - toggle to true
    request.setIgnoreProject(true);
    assertThat(request.isIgnoreProject()).isTrue();

    // act & assert - toggle back to false
    request.setIgnoreProject(false);
    assertThat(request.isIgnoreProject()).isFalse();
  }
}