package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link EnvironmentCommandlet}.
 */
public class EnvironmentCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link EnvironmentCommandlet} run.
   */
  @Test
  public void testRun() {

    // arrange
    String path = "project/workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext(PROJECT_BASIC, path, false);
    EnvironmentCommandlet env = context.getCommandletManager().getCommandlet(EnvironmentCommandlet.class);
    // act
    env.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "MVN_VERSION=\"3.9.1\""); //overwritten by conf
    assertLogMessage(context, IdeLogLevel.INFO, "SOME=\"some-${UNDEFINED}\"");
    assertLogMessage(context, IdeLogLevel.INFO, "BAR=\"bar-some-${UNDEFINED}\"");
    assertLogMessage(context, IdeLogLevel.INFO, "IDE_TOOLS=\"mvn,eclipse\"");
    assertLogMessage(context, IdeLogLevel.INFO, "ECLIPSE_VERSION=\"2023-03\"");
    assertLogMessage(context, IdeLogLevel.INFO, "FOO=\"foo-bar-some-${UNDEFINED}\"");
    assertLogMessage(context, IdeLogLevel.INFO, "JAVA_VERSION=\"17*\"");
    assertLogMessage(context, IdeLogLevel.INFO, "INTELLIJ_EDITION=\"ultimate\"");
    assertLogMessage(context, IdeLogLevel.INFO, "DOCKER_EDITION=\"docker\"");
  }

  /**
   * Test that {@link EnvironmentCommandlet} requires home.
   */
  @Test
  public void testThatHomeIsRequired() {

    // arrange
    EnvironmentCommandlet env = new EnvironmentCommandlet(IdeTestContextMock.get());
    // act & assert
    assertThat(env.isIdeHomeRequired()).isTrue();
  }
}
