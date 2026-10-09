package com.devonfw.tools.ide.url.tool.scalactli;

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
 * Test of {@link ScalaCliUrlUpdater}.
 */
@WireMockTest
public class ScalaCliUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link ScalaCliUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testScalaCliUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {
    // arrange
    stubFor(get(urlMatching("/repos/VirtusLab/scala-cli/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("ScalaCliUrlUpdater")
                .resolve("scala-cli-releases.json"), wmRuntimeInfo))));

    // the release tag carries a "v" prefix (v${version}) and the asset name has no version segment
    stubFor(any(urlMatching("/VirtusLab/scala-cli/releases/download/v\\d+\\.\\d+\\.\\d+/scala-cli-(x86_64-pc-linux\\.gz|x86_64-apple-darwin\\.gz"
        + "|aarch64-apple-darwin\\.gz|x86_64-pc-win32\\.zip)"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    ScalaCliUrlUpdater updater = new ScalaCliUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    // scala-cli publishes a linux x64, mac x64, mac arm64 and windows x64 asset (no linux arm64, no windows arm64)
    List<String> expectedPlatforms = List.of("linux_x64", "mac_x64", "mac_arm64", "windows_x64");
    Path scalaCliDir = tempDir.resolve("scala-cli").resolve("scala-cli");
    assertUrlVersion(scalaCliDir.resolve("1.17.1"), expectedPlatforms);
    assertUrlVersion(scalaCliDir.resolve("1.17.0"), expectedPlatforms);
    assertUrlVersion(scalaCliDir.resolve("1.16.0"), expectedPlatforms);
  }
}
