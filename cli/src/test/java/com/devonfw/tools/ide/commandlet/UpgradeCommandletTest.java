package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

class UpgradeCommandletTest extends AbstractIdeContextTest {

  @Test
  public void testSnapshotVersionComparisons() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    UpgradeCommandlet uc = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // TODO
  }
}
