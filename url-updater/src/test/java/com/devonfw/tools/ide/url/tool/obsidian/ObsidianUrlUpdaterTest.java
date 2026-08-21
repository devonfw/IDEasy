package com.devonfw.tools.ide.url.tool.obsidian;

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
import com.devonfw.tools.ide.url.updater.UpdateManager;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link ObsidianUrlUpdater}.
 */
@WireMockTest
public class ObsidianUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link ObsidianUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testObsidianUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/obsidianmd/obsidian-releases/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("ObsidianUrlUpdater")
                .resolve("obsidian-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/obsidianmd/obsidian-releases/releases/download/v\\d+\\.\\d+\\.\\d+/Obsidian-.*\\.exe"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));
    stubFor(any(urlMatching("/obsidianmd/obsidian-releases/releases/download/v\\d+\\.\\d+\\.\\d+/Obsidian-.*\\.dmg"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));
    stubFor(any(urlMatching("/obsidianmd/obsidian-releases/releases/download/v\\d+\\.\\d+\\.\\d+/obsidian_.*_amd64\\.deb"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));
    stubFor(any(urlMatching("/obsidianmd/obsidian-releases/releases/download/v\\d+\\.\\d+\\.\\d+/obsidian-.*\\.tar\\.gz"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));
    stubFor(any(urlMatching("/obsidianmd/obsidian-releases/releases/download/v\\d+\\.\\d+\\.\\d+/obsidian-.*-arm64\\.tar\\.gz"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UpdateManager updateManager = new UpdateManager(tempDir, null, null);
    UrlRepository urlRepository = updateManager.getUrlRepository();
    ObsidianUrlUpdater updater = new ObsidianUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());
    updater.setUpdateManager(updateManager);
    
    // act
    updater.update(urlRepository);

    // assert
    List<String> expectedPlatforms = List.of("windows_x64", "mac_x64", "linux_x64");
    Path obsidianDir = tempDir.resolve("obsidian").resolve("obsidian");
    assertUrlVersion(obsidianDir.resolve("1.9.10"), expectedPlatforms);
    assertUrlVersion(obsidianDir.resolve("1.10.6"), expectedPlatforms);
    assertUrlVersion(obsidianDir.resolve("1.11.7"), expectedPlatforms);
    assertUrlVersion(obsidianDir.resolve("1.12.7"), expectedPlatforms);
  }
}
