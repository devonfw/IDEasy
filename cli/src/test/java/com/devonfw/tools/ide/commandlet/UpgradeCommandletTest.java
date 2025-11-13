package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
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
    context.getNetworkStatus().simulateNetworkError();
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    CliException e = assertThrows(CliException.class, upgrade::run);
    // assert
    assertThat(e).hasMessage("You are offline but Internet access is required for upgrade of IDEasy");
  }
}
