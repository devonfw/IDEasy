package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UpgradeCommandlet}.
 */
@WireMockTest
class UpgradeCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_NAME = "upgrade";

  @Test
  public void testSnapshotVersionComparisons(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NAME, wmRuntimeInfo);
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    upgrade.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("You are using IDEasy version SNAPSHOT which indicates local development - skipping upgrade.");
  }

  @Test
  public void testUpgradeWhenOffline(WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    IdeTestContext context = newContext(PROJECT_NAME, wmRuntimeInfo);
    context.getNetworkStatus().simulateNetworkError();
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    CliException e = assertThrows(CliOfflineException.class, upgrade::run);
    // assert
    assertThat(e).hasMessage("You are offline but Internet access is required for upgrade of IDEasy");
  }
}
