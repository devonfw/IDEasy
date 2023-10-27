package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;

/**
 * Integration test of {@link VersionListCommandlet}.
 */
public class VersionListCommandletTest extends AbstractIdeContextTest {

  /**
   * Test of {@link VersionListCommandlet} run.
   */
  @Test
  public void testVersionListCommandletRun() {

    // arrange
    String path = "workspaces/foo-test/my-git-repo";
    IdeTestContext context = newContext("basic", path, false);
    VersionListCommandlet versionList = context.getCommandletManager().getCommandlet(VersionListCommandlet.class);
    versionList.tool.setValueAsString("mvn");
    // act
    versionList.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "3.0.5");
    assertLogMessage(context, IdeLogLevel.INFO, "3.1.0");
    assertLogMessage(context, IdeLogLevel.INFO, "3.2.1");
  }
}