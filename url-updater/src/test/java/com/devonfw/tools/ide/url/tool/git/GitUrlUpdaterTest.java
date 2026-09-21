package com.devonfw.tools.ide.url.tool.git;

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
 * Test of {@link GitUrlUpdater}.
 */
@WireMockTest
class GitUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for GitUrlUpdater: verifies that update creates expected files for Git versions.
   */
  @Test
  void testGitUrlUpdaterCreatesDownloadUrlsAndChecksums(
      @TempDir Path tempDir,
      WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/install/windows"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(
                PATH_INTEGRATION_TEST.resolve("GitUrlUpdater").resolve("index.html"),
                wmRuntimeInfo))));

    stubFor(any(urlMatching(
        "/releases/download/v[0-9.]+\\.windows\\.[0-9]+/Git-[0-9.]+-(64-bit|arm64)\\.exe"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    GitUrlUpdater updater = new GitUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path gitEditionPath = tempDir.resolve("git").resolve("git");

    assertUrlVersion(
        gitEditionPath.resolve("2.55.0.3"),
        List.of("windows_x64", "windows_arm64"));

    assertUrlVersion(
        gitEditionPath.resolve("2.54.0.1"),
        List.of("windows_x64", "windows_arm64"));

    assertUrlVersion(
        gitEditionPath.resolve("2.53.0.2"),
        List.of("windows_x64", "windows_arm64"));
  }
}
