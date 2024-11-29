package com.devonfw.tools.ide.commandlet;

import org.junit.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

public class UpgradeSettingsCommandletTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "upgrade-settings";
  private final IdeTestContext context = newContext(PROJECT_INTELLIJ);

  @Test
  public void testDevonPropertiesUpgrade() {
    //arrange
    UpgradeSettingsCommandlet upgradeSettingsCommandlet = new UpgradeSettingsCommandlet(context);
    //act
    upgradeSettingsCommandlet.run();
  }
}
