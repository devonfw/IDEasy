package com.devonfw.tools.ide.commandlet;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.context.IdeTestContextMock;
import com.devonfw.tools.ide.log.IdeLogEntry;

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
    Path settingsIdeProperties = context.getSettingsPath().resolve("ide.properties");
    Path confIdeProperties = context.getConfPath().resolve("ide.properties");
    // act
    env.run();
    // assert
    assertThat(context).log().hasEntriesWithNothingElseInBetween( //
        IdeLogEntry.ofDebug("from defaults:"), IdeLogEntry.ofInfo("DOCKER_EDITION=\"docker\""), IdeLogEntry.ofInfo("INTELLIJ_EDITION=\"ultimate\""), //
        IdeLogEntry.ofDebug("from " + settingsIdeProperties + ":"), IdeLogEntry.ofInfo("JAVA_VERSION=\"17*\""),
        IdeLogEntry.ofInfo("SOME=\"some-${UNDEFINED}\""), IdeLogEntry.ofInfo("BAR=\"bar-some-${UNDEFINED}\""),
        IdeLogEntry.ofInfo("IDE_TOOLS=\"mvn,eclipse\""),
        IdeLogEntry.ofDebug("from " + confIdeProperties + ":"), IdeLogEntry.ofInfo("MVN_VERSION=\"3.9.1\"") //overwritten by conf
    );
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
