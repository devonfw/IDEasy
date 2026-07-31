package com.devonfw.tools.ide.commandlet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.cli.CliOfflineException;
import com.devonfw.tools.ide.completion.CompletionCandidate;
import com.devonfw.tools.ide.completion.CompletionCandidateCollector;
import com.devonfw.tools.ide.completion.CompletionCandidateCollectorDefault;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.property.EnumProperty;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link UpgradeCommandlet}.
 */
@WireMockTest
class UpgradeCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_NAME = "upgrade";

  @Test
  void testSnapshotVersionComparisons(WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_NAME, wmRuntimeInfo);
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    upgrade.run();

    // assert
    assertThat(context).logAtWarning().hasMessage("You are using IDEasy version SNAPSHOT which indicates local development - skipping upgrade.");
  }

  @Test
  void testUpgradeWhenOffline(WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    IdeTestContext context = newContext(PROJECT_NAME, wmRuntimeInfo);
    context.getNetworkStatus().simulateNetworkError();
    UpgradeCommandlet upgrade = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    // act
    CliException e = assertThrows(CliOfflineException.class, upgrade::run);
    // assert
    assertThat(e).hasMessage("You are offline but Internet access is required for upgrade of IDEasy");
  }

  @ParameterizedTest
  @CsvSource({ "'',stable unstable snapshot", "u,unstable", "s,stable snapshot", "st,stable" })
  void testUpgradeModeCompletion(String input, String expected) {
    // arrange
    IdeTestContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    EnumProperty<UpgradeMode> property = new EnumProperty<>("--mode", false, null, UpgradeMode.class);

    // act
    property.completeValue(input, context, new UpgradeCommandlet(context), collector);

    // assert
    assertThat(collector.getCandidates().stream().map(CompletionCandidate::text))
        .containsExactly(expected.split(" "));
  }

  /**
   * Test that no completion candidates are suggested for an unknown prefix.
   */
  @Test
  void testUpgradeModeCompletionNoMatch() {
    // arrange
    IdeContext context = new IdeTestContext();
    CompletionCandidateCollector collector = new CompletionCandidateCollectorDefault(context);
    EnumProperty<UpgradeMode> property = new EnumProperty<>("--mode", false, null, UpgradeMode.class);

    // act
    property.completeValue("ss", context, new UpgradeCommandlet(context), collector);

    // assert
    assertThat(collector.getCandidates()).isEmpty();
  }
}
