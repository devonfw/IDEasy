package com.devonfw.tools.ide.url.tool.gcloud;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link GCloudUrlUpdater}.
 */
@WireMockTest
class GCloudUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Tests version discovery from the official manifest and the creation of platform-specific download URLs and checksums.
   *
   * @param version the version reported by the manifest.
   * @param arm whether ARM archives are expected for this version.
   * @param tempDir the temporary URL repository.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @ParameterizedTest
  @CsvSource({"300.0.0, false", "366.0.0, false", "590.0.1, true"})
  void testGCloudUrlUpdaterCreatesDownloadUrlsAndChecksums(String version, boolean arm, @TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    String versionPath = "/dl/cloudsdk/channels/rapid/components-2.json";
    String response = readAndResolve(PATH_INTEGRATION_TEST.resolve("GCloudUrlUpdater").resolve("gcloud-version.json"), wmRuntimeInfo)
        .replace("590.0.1", version);
    stubFor(get(urlEqualTo(versionPath)).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(response)));
    stubFor(any(urlPathMatching("/dl/cloudsdk/channels/rapid/downloads/.*"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));
    UrlRepository urlRepository = UrlRepository.load(tempDir);
    GCloudUrlUpdater updater = new GCloudUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path versionFolder = tempDir.resolve("gcloud").resolve("gcloud").resolve(version);
    assertUrlVersionOsX64(versionFolder);
    String archive = "google-cloud-sdk-" + version + "-";
    assertThat(versionFolder.resolve("windows_x64.urls")).content().contains(archive + "windows-x86_64-bundled-python.zip");
    assertThat(versionFolder.resolve("linux_x64.urls")).content().contains(archive + "linux-x86_64.tar.gz");
    assertThat(versionFolder.resolve("mac_x64.urls")).content().contains(archive + "darwin-x86_64.tar.gz");
    if (arm) {
      assertUrlVersionFile(versionFolder, "linux_arm64");
      assertUrlVersionFile(versionFolder, "mac_arm64");
      assertThat(versionFolder.resolve("linux_arm64.urls")).content().contains(archive + "linux-arm.tar.gz");
      assertThat(versionFolder.resolve("mac_arm64.urls")).content().contains(archive + "darwin-arm.tar.gz");
    } else {
      assertThat(versionFolder.resolve("linux_arm64.urls")).doesNotExist();
      assertThat(versionFolder.resolve("mac_arm64.urls")).doesNotExist();
    }
    verify(1, getRequestedFor(urlEqualTo(versionPath)));
  }

}
