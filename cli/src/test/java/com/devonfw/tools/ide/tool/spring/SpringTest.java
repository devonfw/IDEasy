package com.devonfw.tools.ide.tool.spring;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Integration test of {@link Spring}.
 */
@WireMockTest
public class SpringTest extends AbstractIdeContextTest {

  private static final String PROJECT_SPRING = "spring";


  /**
   * Tests if {@link Spring} can be installed properly.
   */
  @Test
  public void testSpringInstall(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SPRING, wireMockRuntimeInfo);
    Spring commandlet = new Spring(context);

    MvnRepository mavenRepo = new MvnRepository(context);
//    UrlDownloadFileMetadata metadata = mavenRepo.getMetadata(tool, edition, version, toolCommandlet);
    context.info("Starting testSpringInstall");
    // act
    commandlet.install();

    // assert
    checkInstallation(context);
  }

  /**
   * Tests if {@link Spring spring-boot-cli} can be run.
   */
  @Test
  public void testSpringRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_SPRING, wireMockRuntimeInfo);
    Spring commandlet = new Spring(context);
    context.info("Starting testSpringRun");
    commandlet.arguments.addValue("foo");

    // act
    commandlet.run();

    // assert
    checkInstallation(context);
    assertThat(context).logAtInfo().hasMessage("spring " + "foo");
  }

  private void checkInstallation(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("spring/.ide.software.version")).exists().hasContent("2.4.0");
    assertThat(context).logAtSuccess().hasEntries("Successfully installed java in version 17.0.10_7",
        "Successfully installed spring in version 2.4.0");
  }
}
