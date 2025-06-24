package com.devonfw.tools.ide.url.tool.java;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.tool.npm.NpmUrlUpdater;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for integrations of the {@link NpmUrlUpdater}
 */
@WireMockTest
public class JavaUrlUpdaterTest extends Assertions {

  /**
   * Test resource location
   */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/JavaJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link JavaUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testJavaUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    stubFor(get(urlMatching("/versions/")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("java-version.json")))));

    stubFor(any(urlMatching("/downloads/.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    JavaUrlUpdaterMock updater = new JavaUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    Path JavaVersionsPath2103 = tempDir.resolve("java").resolve("java").resolve("21.0.3_9");
    Path JavaVersionsPath2201 = tempDir.resolve("java").resolve("java").resolve("22.0.1_8");
    Path JavaVersionsPath2236 = tempDir.resolve("java").resolve("java").resolve("22_36");

    // then
    assertThat(JavaVersionsPath2103.resolve("status.json")).exists();
    assertThat(JavaVersionsPath2103.resolve("windows_x64.urls")).exists();
    assertThat(JavaVersionsPath2103.resolve("windows_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2103.resolve("linux_x64.urls")).exists();
    assertThat(JavaVersionsPath2103.resolve("linux_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2103.resolve("mac_x64.urls")).exists();
    assertThat(JavaVersionsPath2103.resolve("mac_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2103.resolve("mac_arm64.urls")).exists();
    assertThat(JavaVersionsPath2103.resolve("mac_arm64.urls.sha256")).exists();

    assertThat(JavaVersionsPath2201.resolve("status.json")).exists();
    assertThat(JavaVersionsPath2201.resolve("windows_x64.urls")).exists();
    assertThat(JavaVersionsPath2201.resolve("windows_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2201.resolve("linux_x64.urls")).exists();
    assertThat(JavaVersionsPath2201.resolve("linux_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2201.resolve("mac_x64.urls")).exists();
    assertThat(JavaVersionsPath2201.resolve("mac_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2201.resolve("mac_arm64.urls")).exists();
    assertThat(JavaVersionsPath2201.resolve("mac_arm64.urls.sha256")).exists();

    assertThat(JavaVersionsPath2236.resolve("status.json")).exists();
    assertThat(JavaVersionsPath2236.resolve("windows_x64.urls")).exists();
    assertThat(JavaVersionsPath2236.resolve("windows_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2236.resolve("linux_x64.urls")).exists();
    assertThat(JavaVersionsPath2236.resolve("linux_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2236.resolve("mac_x64.urls")).exists();
    assertThat(JavaVersionsPath2236.resolve("mac_x64.urls.sha256")).exists();
    assertThat(JavaVersionsPath2236.resolve("mac_arm64.urls")).exists();
    assertThat(JavaVersionsPath2236.resolve("mac_arm64.urls.sha256")).exists();

  }

}
