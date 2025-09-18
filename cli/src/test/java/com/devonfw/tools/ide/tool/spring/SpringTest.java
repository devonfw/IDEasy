package com.devonfw.tools.ide.tool.spring;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Spring}.
 */
@WireMockTest
public class SpringTest extends AbstractIdeContextTest {

  private static final String PROJECT_SPRING = "spring";
  private final IdeTestContext context = newContext(PROJECT_SPRING);

  /**
   * Tests if the {@link Spring} can be installed properly.
   */
  @Test
  public void testSpringInstall() {

    // arrange
    Spring commandlet = new Spring(this.context);
    this.context.info("Starting testSpringInstall");

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
  }

  /**
   * Tests if {@link Spring spring-boot-cli} can be run.
   */
  @Test
  public void testSpringRun() {

    // arrange
    Spring commandlet = new Spring(this.context);
    this.context.info("Starting testSpringRun");
    commandlet.arguments.addValue("foo");

    // act
    commandlet.run();

    // assert
    checkInstallation(this.context);
    assertThat(context).logAtInfo().hasMessage("spring " + "foo");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("spring/.ide.software.version")).exists().hasContent("5.3.3");
    assertThat(context).logAtSuccess().hasEntries("Successfully installed java in version 17.0.10_7",
        "Successfully installed spring in version 5.3.3");
  }
}
