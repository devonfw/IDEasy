package com.devonfw.tools.ide.commandlet;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Test of {@link UpgradeCommandlet}.
 */
class UpgradeCommandletTest extends AbstractIdeContextTest {

  @Test
  public void testSnapshotVersionComparisons() {

    // arrange
    IdeTestContext context = new IdeTestContext();
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    upgrade.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("You are using IDEasy version SNAPSHOT which indicates local development - skipping upgrade.");
  }

  @Test
  public void testUpgradeWhenOffline() {
    // arrange
    IdeTestContext context = new IdeTestContext();
    context.setOnline(false);
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    upgrade.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("You are offline. IDEasy requires an internet connection to upgrade - skipping upgrade.");
  }
}
