package com.devonfw.tools.ide.tool.mvnd;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Mvnd}.
 */
@WireMockTest
class MvndTest extends AbstractIdeContextTest {

  private static final String MVND_PROJECT = "mvnd";

  @Test
  void testMvndBasicProperties(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MVND_PROJECT, wireMockRuntimeInfo);
    Mvnd mvnd = new Mvnd(context);

    // assert
    assertThat(mvnd.getName()).isEqualTo("mvnd");
    assertThat(mvnd.getTags()).contains(Tag.JAVA, Tag.BUILD);
    assertThat(mvnd.getToolHelpArguments()).isEqualTo("--help");
  }

  @Test
  void testMvndInstallAndRun(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(MVND_PROJECT, wireMockRuntimeInfo);
    Mvnd mvnd = new Mvnd(context);

    // act
    mvnd.install();

    // assert
    assertThat(context.getSoftwarePath().resolve("mvnd/bin/mvnd")).exists();
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed mvnd");

    // act
    mvnd.arguments.addValue("foo");
    mvnd.arguments.addValue("bar");
    mvnd.run();

    // assert
    assertThat(context).logAtInfo().hasMessageContaining("mvnd foo bar");
  }

}
