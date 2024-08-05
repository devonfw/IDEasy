package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

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
    assertThat(context).logAtInfo().hasEntries("3.2.1", "3.1.0", "3.0.5");
  }
}
