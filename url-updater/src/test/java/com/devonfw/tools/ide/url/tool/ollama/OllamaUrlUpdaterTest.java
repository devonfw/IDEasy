package com.devonfw.tools.ide.url.tool.ollama;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link OllamaUrlUpdater}.
 */
@WireMockTest
class OllamaUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link OllamaUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testOllamaUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    stubFor(get(urlMatching("/repos/ollama/ollama/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("OllamaUrlUpdater")
                .resolve("ollama-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching(
        "/ollama/ollama/releases/download/[^/]+/ollama-(darwin\\.tgz|windows-(amd64|arm64)\\.zip)"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    OllamaUrlUpdater updater = new OllamaUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    List<String> expectedPlatforms = List.of("mac_x64", "mac_arm64", "windows_x64", "windows_arm64");
    Path ollamaDir = tempDir.resolve("ollama").resolve("ollama");
    assertUrlVersion(ollamaDir.resolve("0.35.0"), expectedPlatforms);
  }
}
