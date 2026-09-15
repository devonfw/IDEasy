package com.devonfw.tools.ide.url.tool.agy;

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
 * Test of {@link AgyUrlUpdater}.
 */
@WireMockTest
public class AgyUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link AgyUrlUpdater} for the creation of download URLs and checksums
   *
   * @param tempDir Path to temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testAgyUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    stubFor(get(urlMatching("/repos/google-antigravity/antigravity-cli/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("AgyUrlUpdater")
                .resolve("agy-release.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching(
        "/google-antigravity/antigravity-cli/releases/download/\\d+\\.\\d+\\.\\d+/agy_cli_(windows|mac|linux)_(x64|arm64)\\.(zip|tar\\.gz)"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AgyUrlUpdater updater = new AgyUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    List<String> expectedPlatforms = List.of("windows_x64", "windows_arm64", "mac_x64", "mac_arm64", "linux_x64", "linux_arm64");
    Path agyDir = tempDir.resolve("agy").resolve("agy");
    assertUrlVersion(agyDir.resolve("1.2.3"), expectedPlatforms);
    assertUrlVersion(agyDir.resolve("1.2.2"), expectedPlatforms);
    assertUrlVersion(agyDir.resolve("1.2.1"), expectedPlatforms);
    assertUrlVersion(agyDir.resolve("1.2.0"), expectedPlatforms);

  }
}
