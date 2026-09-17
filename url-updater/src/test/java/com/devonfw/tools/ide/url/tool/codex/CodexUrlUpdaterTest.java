package com.devonfw.tools.ide.url.tool.codex;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link CodexUrlUpdater}.
 */
@WireMockTest
class CodexUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link CodexUrlUpdater} for the creation of download URLs and checksums. Verifies that all supported OS/architecture combinations are downloaded
   * for the stable Rust CLI releases and that foreign (Python SDK) and pre-release (alpha) releases are filtered out.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testCodexUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/openai/codex/releases")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("CodexUrlUpdater").resolve("codex-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/openai/codex/releases/download/.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    CodexUrlUpdater updater = new CodexUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path codexDir = tempDir.resolve("codex").resolve("codex");
    assertUrlVersionOsArch(codexDir.resolve("0.154.0"));
    assertUrlVersionOsArch(codexDir.resolve("0.153.0"));
    // pre-release (alpha) and foreign (Python SDK) releases must be filtered out
    assertThat(codexDir.resolve("0.155.0-alpha.9")).doesNotExist();
    assertThat(codexDir.resolve("Python SDK 0.154.0")).doesNotExist();
  }
}
