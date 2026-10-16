package com.devonfw.tools.ide.url.tool.snyk;

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

@WireMockTest
class SnykUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link SnykUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testSnykUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/snyk/cli/releases")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("SnykUrlUpdater").resolve("snyk-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/snyk/cli/releases/download/v.*"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    SnykUrlUpdater updater = new SnykUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert (Main 5: linux x64 + arm64, mac x64 + arm64, windows x64)
    Path snykDir = tempDir.resolve("snyk").resolve("snyk");
    assertUrlVersion(snykDir.resolve("1.1307.4"),
        List.of("linux_x64", "linux_arm64", "mac_x64", "mac_arm64", "windows_x64"));
  }
}
