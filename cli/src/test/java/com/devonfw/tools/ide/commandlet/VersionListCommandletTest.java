package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import org.junit.jupiter.api.Test;

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
    IdeTestContext context = newContext(PROJECT_BASIC, null, false);
    VersionListCommandlet versionList = context.getCommandletManager().getCommandlet(VersionListCommandlet.class);
    versionList.tool.setValueAsString("mvn", context);
    // act
    versionList.run();
    // assert
    assertLogMessage(context, IdeLogLevel.INFO, "3.0.5");
    assertLogMessage(context, IdeLogLevel.INFO, "3.1.0");
    assertLogMessage(context, IdeLogLevel.INFO, "3.2.1");
  }
}