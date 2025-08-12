package com.devonfw.tools.ide.url.tool.docker;

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
 * Test class for integrations of the {@link DockerDesktopUrlUpdater}.
 */
@WireMockTest
public class DockerDesktopUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for {@link DockerDesktopUrlUpdater}: verifies that update creates expected files for DockerDesktop versions.
   */
  @Test
  void testDockerDesktopUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/desktop/release-notes/")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("DockerDesktopUrlUpdater").resolve("index.html"), wmRuntimeInfo))));
    stubFor(
        any(urlMatching("/(win|mac)/main/(amd64|arm64)/[0-9.]+/D.*"))
            .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));
    stubFor(
        any(urlMatching("/.*/checksums.txt"))
            .willReturn(
                aResponse().withStatus(200).withBody(SHA_256)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    DockerDesktopUrlUpdater updater = new DockerDesktopUrlUpdaterMock(wmRuntimeInfo);
    // act
    updater.update(urlRepository);

    // assert
    Path dockerDesktopEditionPath = tempDir.resolve("docker").resolve("docker");
    for (String version : new String[] { "4.30.0", "4.34.0" }) {
      Path pgadminVersionPath = dockerDesktopEditionPath.resolve(version);
      assertUrlVersion(pgadminVersionPath, List.of("windows_x64", "windows_arm64", "mac_x64", "mac_arm64"));
    }
  }
}
