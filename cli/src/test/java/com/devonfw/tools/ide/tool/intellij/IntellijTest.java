package com.devonfw.tools.ide.tool.intellij;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.tool.jmc.Jmc;
import org.junit.jupiter.api.Test;

/**
 * Integration test of {@link Jmc}.
 */
public class IntellijTest extends AbstractIdeContextTest {

  private static final String PROJECT_INTELLIJ = "intellij";

  @Test
  public void testIntellijInstall() {

    // arrange
    IdeTestContext context = newContext(PROJECT_INTELLIJ);
    Intellij commandlet = new Intellij(context);

    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  private void checkInstallation(IdeTestContext context) {

    if (context.getSystemInfo().isLinux()) {
      assertThat(context.getSoftwarePath().resolve("intellij/bin/idea")).exists().isSymbolicLink();
    }
    assertThat(context.getSoftwarePath().resolve("intellij/.ide.software.version")).exists().hasContent("2023.3.3");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed java in version 17.0.10_7");
    assertLogMessage(context, IdeLogLevel.SUCCESS, "Successfully installed intellij in version 2023.3.3");
  }
}
