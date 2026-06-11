package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link CleanupCommandlet}.
 */
class CleanupCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_BASIC = "basic";

  /**
   * Test of {@link CleanupCommandlet} that {@code az} tool is not used by any project and thus deleted.
   */
  @Test
  void testCleanupDeletesUnusedGlobalSoftware() {

    // arrange
    IdeTestContext context = newContext(PROJECT_BASIC);
    CleanupCommandlet cleanup = context.getCommandletManager().getCommandlet(CleanupCommandlet.class);
    cleanup.forceDelete.setValue(true);

    // act
    cleanup.run();

    // assert
    assertThat(context.getIdeRoot().resolve("_ide/software/default/az")).doesNotExist();
    assertThat(context).logAtSuccess().hasMessage("Unused tools have been deleted successfully.");
  }
}
