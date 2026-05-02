package com.devonfw.tools.ide.url.tool.claude;

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
 * Test of {@link ClaudeUrlUpdater}.
 */
@WireMockTest
class ClaudeUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link ClaudeUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testClaudeUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/anthropics/claude-code/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("ClaudeUrlUpdater").resolve("claude-tags.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/anthropics/claude-code/releases/download/.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    ClaudeUrlUpdaterMock updater = new ClaudeUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path claudeDir = tempDir.resolve("claude").resolve("claude");
    assertUrlVersionOsX64(claudeDir.resolve("2.1.118"));
  }
}


