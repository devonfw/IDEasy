package com.devonfw.tools.ide.url.tool.pnpm;

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
 * Test of {@link PnpmUrlUpdater}.
 */
@WireMockTest
public class PnpmUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link PnpmUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testPnpmUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    stubFor(get(urlMatching("/repos/pnpm/pnpm/git/refs/tags"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("PnpmUrlUpdater").resolve("pnpm-tags.json"), wmRuntimeInfo))));

    // the git tag carries a "v" prefix (v${version}) and the asset name has no version segment
    stubFor(any(urlMatching("/pnpm/pnpm/releases/download/v\\d+\\.\\d+\\.\\d+/pnpm-(linux-x64\\.tar\\.gz|linux-arm64\\.tar\\.gz|darwin-x64\\.tar\\.gz"
        + "|darwin-arm64\\.tar\\.gz|win32-x64\\.zip|win32-arm64\\.zip)"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PnpmUrlUpdater updater = new PnpmUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    // pnpm publishes a linux x64/arm64, mac x64/arm64 and windows x64/arm64 asset
    List<String> expectedPlatforms = List.of("linux_x64", "linux_arm64", "mac_x64", "mac_arm64", "windows_x64", "windows_arm64");
    Path pnpmDir = tempDir.resolve("pnpm").resolve("pnpm");
    assertUrlVersion(pnpmDir.resolve("12.12.1"), expectedPlatforms);
    assertUrlVersion(pnpmDir.resolve("12.11.0"), expectedPlatforms);
    assertUrlVersion(pnpmDir.resolve("11.0.0"), expectedPlatforms);

    // versions below the minimum (pre-v11, which used a different asset naming) and pre-release versions are not registered
    Assertions.assertThat(pnpmDir.resolve("10.34.7")).doesNotExist();
    Assertions.assertThat(pnpmDir.resolve("8.6.0")).doesNotExist();
    Assertions.assertThat(pnpmDir.resolve("12.13.0-alpha.1")).doesNotExist();
  }
}
