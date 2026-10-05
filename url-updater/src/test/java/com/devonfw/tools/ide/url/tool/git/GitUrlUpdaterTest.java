package com.devonfw.tools.ide.url.tool.git;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

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
   * Tests {@link GitUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir path to temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testGitUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/git-for-windows/git/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(
                PATH_INTEGRATION_TEST.resolve("GitUrlUpdater").resolve("git-releases.json"),
                wmRuntimeInfo))));

    stubFor(any(urlMatching(
        "/git-for-windows/git/releases/download/v[0-9.]+\\.windows\\.[0-9]+/Git-[0-9.]+-(64-bit|arm64)\\.exe"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    GitUrlUpdater updater = new GitUrlUpdater(
        wmRuntimeInfo.getHttpBaseUrl(),
        wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    List<String> expectedPlatforms = List.of(
        "windows_x64",
        "windows_arm64");

    Path gitDir = tempDir.resolve("git").resolve("git");

    assertUrlVersion(
        gitDir.resolve("2.56.0.2"),
        expectedPlatforms);

    assertUrlVersion(
        gitDir.resolve("2.56.0.1"),
        expectedPlatforms);

    assertUrlVersion(
        gitDir.resolve("2.55.0.4"),
        expectedPlatforms);

    verify(getRequestedFor(urlEqualTo(
        "/git-for-windows/git/releases/download/v2.56.0.windows.1/Git-2.56.0-64-bit.exe")));

    verify(getRequestedFor(urlEqualTo(
        "/git-for-windows/git/releases/download/v2.56.0.windows.1/Git-2.56.0-arm64.exe")));

    verify(getRequestedFor(urlEqualTo(
        "/git-for-windows/git/releases/download/v2.56.0.windows.2/Git-2.56.0.2-64-bit.exe")));

    verify(getRequestedFor(urlEqualTo(
        "/git-for-windows/git/releases/download/v2.56.0.windows.2/Git-2.56.0.2-arm64.exe")));
  }
}
