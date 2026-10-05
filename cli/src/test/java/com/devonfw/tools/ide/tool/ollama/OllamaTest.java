package com.devonfw.tools.ide.tool.ollama;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link Ollama}.
 */
@WireMockTest
class OllamaTest extends AbstractIdeContextTest {

  private static final String PROJECT_OLLAMA = "ollama";

  private static final String OLLAMA_VERSION = "0.35.0";

  @Test
  void testOllamaInstallSucceedsViaHttpDownload(WireMockRuntimeInfo wireMockRuntimeInfo) {

    // arrange
    IdeTestContext context = newContext(PROJECT_OLLAMA, wireMockRuntimeInfo);
    Ollama ollama = new Ollama(context);

    // act
    ollama.install();

    // assert
    assertInstalled(context);
  }

  private void assertInstalled(IdeTestContext context) {

    assertThat(context.getSoftwarePath().resolve("ollama/.ide.software.version")).exists().hasContent(OLLAMA_VERSION);
    assertThat(context).logAtSuccess().hasMessageContaining("Successfully installed ollama in version " + OLLAMA_VERSION);
  }
}
