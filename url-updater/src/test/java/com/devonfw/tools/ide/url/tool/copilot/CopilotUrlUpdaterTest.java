package com.devonfw.tools.ide.url.tool.copilot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link CopilotUrlUpdater}.
 */
@WireMockTest
class CopilotUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link CopilotUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testCopilotUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/github/copilot-cli/releases")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("CopilotUrlUpdater").resolve("copilot-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/github/copilot-cli/releases/download/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    CopilotUrlUpdaterMock updater = new CopilotUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path copilotDir = tempDir.resolve("copilot").resolve("copilot");
    assertUrlVersionOsX64(copilotDir.resolve("1.0.22-0"));
    assertUrlVersionOsX64(copilotDir.resolve("1.0.21"));
  }

}

