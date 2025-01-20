package com.devonfw.tools.ide.commandlet;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpgradeCommandletTest extends AbstractIdeContextTest {

  @Test
  public void testSnapshotVersionComparisons() {

    IdeTestContext context = newContext(PROJECT_BASIC);
    UpgradeCommandlet uc = context.getCommandletManager().getCommandlet(UpgradeCommandlet.class);

    assertThat(uc.isSnapshotNewer("2024.12.002-beta-12_18_02-SNAPSHOT", "2025.01.001-beta-20250118.022832-8")).isTrue();
    assertThat(uc.isSnapshotNewer("2024.12.002-beta-01_01_02-SNAPSHOT", "2024.12.002-beta-20241218.023429-8")).isTrue();
    assertThat(
        uc.isSnapshotNewer("2024.12.002-beta-12_18_02-SNAPSHOT", "2024.12.002-beta-20241218.023429-8")).isFalse();
    assertThat(uc.isSnapshotNewer("SNAPSHOT", "2024.12.002-beta-20241218.023429-8")).isFalse();
    assertThat(uc.isSnapshotNewer("someUnknownFormat1", "someUnknownFormat2")).isFalse();
  }
}