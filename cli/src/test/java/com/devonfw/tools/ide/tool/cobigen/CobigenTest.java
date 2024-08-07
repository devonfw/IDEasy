package com.devonfw.tools.ide.tool.cobigen;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;

/**
 * Integration test of {@link Cobigen}.
 */
public class CobigenTest extends AbstractIdeContextTest {

  private static final String COBIGEN = "cobigen";

  private final IdeTestContext context = newContext(COBIGEN);

  /**
   * Tests if {@link Cobigen} can be installed properly.
   */
  @Test
  public void testCobigenInstall() {

    // arrange
    IdeTestContext context = newContext(COBIGEN);

    Cobigen commandlet = new Cobigen(context);

    // act
    commandlet.install(null);

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if {@link Cobigen} can be run properly.
   */
  @Test
  public void testCobigenRun() {
    // arrange

    Cobigen commandlet = new Cobigen(this.context);

    // act
    commandlet.run();

    // assert
    assertThat(this.context).logAtInfo().hasMessage(COBIGEN + " ");

    checkInstallation(this.context);
  }

  private void checkInstallation(IdeTestContext context) {

    // install - java
    assertThat(context.getSoftwarePath().resolve("java/bin/java")).exists();
    // install - mvn
    assertThat(context.getSoftwarePath().resolve("mvn/bin/mvn")).exists();

    // commandlet - cobigen
    assertThat(context.getSoftwarePath().resolve("cobigen/.ide.software.version")).exists().hasContent("2021.12.006");
    assertThat(context).logAtSuccess().hasMessage("Successfully installed cobigen in version 2021.12.006");
  }
}
