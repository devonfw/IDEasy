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
   * Verify that a dependency request inherits the {@link ToolInstallTracker} from its parent request so that a shared tool installed earlier in the process
   * (as a dependency of another tool) can be recognized and skipped.
   */
  @Test
  void testTrackerIsInherited() {
    // arrange
    ToolInstallTracker tracker = new ToolInstallTracker();
    ToolInstallRequest parent = new ToolInstallRequest(false);
    parent.setTracker(tracker);

    // act
    ToolInstallRequest dependencyRequest = new ToolInstallRequest(parent);

    // assert
    assertThat(dependencyRequest.getTracker()).isSameAs(tracker);
  }

  /**
   * A top-level request (no parent) starts without a tracker; a second {@link ToolInstallRequest#setTracker(ToolInstallTracker) setTracker} call must fail.
   */
  @Test
  void testTrackerSetOnlyOnce() {
    // arrange
    ToolInstallRequest request = new ToolInstallRequest(false);
    assertThat(request.getTracker()).isNull();

    // act + assert
    request.setTracker(new ToolInstallTracker());
    assertThat(request.getTracker()).isNotNull();
    assertThatIllegalStateException().isThrownBy(() -> request.setTracker(new ToolInstallTracker()));
  }
}
