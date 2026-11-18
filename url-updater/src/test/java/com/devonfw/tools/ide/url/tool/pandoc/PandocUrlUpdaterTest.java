package com.devonfw.tools.ide.url.tool.pandoc;

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
 * Test of {@link PandocUrlUpdater}.
 */
@WireMockTest
class PandocUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link PandocUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testPandocUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/jgm/pandoc/releases")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("PandocUrlUpdater").resolve("pandoc-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/jgm/pandoc/releases/download/.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PandocUrlUpdater updater = new PandocUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path pandocDir = tempDir.resolve("pandoc").resolve("pandoc");
    assertUrlVersion(pandocDir.resolve("3.12"), List.of("windows_x64", "mac_x64", "mac_arm64", "linux_x64", "linux_arm64"));
    assertUrlVersion(pandocDir.resolve("3.1.2"), List.of("windows_x64", "mac_x64", "mac_arm64", "linux_x64", "linux_arm64"));
    assertThat(pandocDir.resolve("3.1.1")).doesNotExist();
  }
}
