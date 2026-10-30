package com.devonfw.tools.ide.tool.kubelinter;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
public class KubeLinterTest extends AbstractIdeContextTest {

  private static final String KUBE_LINTER_PROJECT = "kube-linter";

  private static final String KUBE_LINTER_VERSION = "0.8.3";

  @Test
  void testKubeLinterInstallSucceedsOnAllPlatformsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(KUBE_LINTER_PROJECT, wireMockRuntimeInfo);
    KubeLinter kubeLinter = new KubeLinter(context);

    // act
    kubeLinter.install();

    // assert
    assertInstalled(context);
  }

  @Test
  void testKubeLinterRunInstallsAndPassesArguments(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(KUBE_LINTER_PROJECT, wireMockRuntimeInfo);
    KubeLinter kubeLinter = new KubeLinter(context);
    kubeLinter.arguments.addValue("/path/to/manifest.yaml");

    // act
    kubeLinter.run();

    // assert
    assertInstalled(context);
    assertThat(context).logAtInfo().hasMessage("kube-linter /path/to/manifest.yaml");
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("kube-linter/.ide.software.version")).exists().hasContent(KUBE_LINTER_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed kube-linter in version " + KUBE_LINTER_VERSION);
  }

}
