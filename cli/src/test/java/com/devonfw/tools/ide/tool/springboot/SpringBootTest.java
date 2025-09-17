package com.devonfw.tools.ide.tool.springboot;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link SpringBoot}.
 */
@WireMockTest
public class SpringBootTest extends AbstractIdeContextTest {

  private static final String PROJECT_SPRINGBOOT = "springboot";
  private final IdeTestContext context = newContext(PROJECT_SPRINGBOOT);

  /**
   * Tests if the {@link SpringBoot} can be installed properly.
   */
  @Test
  public void testSpringBootInstall() {

    // arrange
    SpringBoot commandlet = new SpringBoot(this.context);
    this.context.info("Starting testSpringBootInstall");

    // act
    commandlet.install();

    // assert
    checkInstallation(this.context);
  }

  /**
   * Tests if {@link SpringBoot spring-boot-cli} can be run.
   */
  @Test
  public void testSpringBootRun() {

    // arrange
    SpringBoot commandlet = new SpringBoot(this.context);
    this.context.info("Starting testSpringBootRun");
    commandlet.arguments.addValue("foo");

    // act
    commandlet.run();

    // assert
    checkInstallation(this.context);
    assertThat(context).logAtInfo().hasMessage("springboot " + "foo");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("springboot/.ide.software.version")).exists().hasContent("5.3.3");
    assertThat(context).logAtSuccess().hasEntries("Successfully installed java in version 17.0.10_7",
        "Successfully installed springboot in version 5.3.3");
  }
}
