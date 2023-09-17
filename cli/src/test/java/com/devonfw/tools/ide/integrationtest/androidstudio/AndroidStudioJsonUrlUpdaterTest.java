package com.devonfw.tools.ide.integrationtest.androidstudio;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.url.updater.androidstudio.AndroidStudioUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for integrations of the {@link AndroidStudioUrlUpdater}.
 */
@WireMockTest(httpPort = 8080)
public class AndroidStudioJsonUrlUpdaterTest extends Assertions {

  /**
   * Test resource location
   */
  private final static String testdataRoot = "src/test/resources/integrationtest/AndroidStudioJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link AndroidStudioUrlUpdater} to download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Paths.get(testdataRoot).resolve("android-version.json")))));

    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*"))
        .willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");

    // then
    assertThat(androidStudioVersionsPath.resolve("status.json")).exists();
    assertThat(androidStudioVersionsPath.resolve("linux_x64.urls")).exists();
    assertThat(androidStudioVersionsPath.resolve("linux_x64.urls.sha256")).exists();
    assertThat(androidStudioVersionsPath.resolve("mac_arm64.urls")).exists();
    assertThat(androidStudioVersionsPath.resolve("mac_arm64.urls.sha256")).exists();
    assertThat(androidStudioVersionsPath.resolve("mac_x64.urls")).exists();
    assertThat(androidStudioVersionsPath.resolve("mac_x64.urls.sha256")).exists();
    assertThat(androidStudioVersionsPath.resolve("windows_x64.urls")).exists();
    assertThat(androidStudioVersionsPath.resolve("windows_x64.urls.sha256")).exists();

  }

  /**
   * Test if {@link AndroidStudioUrlUpdater} can handle downloads with missing checksums (generate checksum from
   * download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir)
      throws IOException {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Paths.get(testdataRoot).resolve("android-version.json")))));

    stubFor(get(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");

    // then
    assertThat(androidStudioVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link AndroidStudioUrlUpdater} can handle downloads with missing checksums
   * (generate checksum from download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Paths.get(testdataRoot).resolve("android-version-without-checksum.json")))));

    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*"))
        .willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");

    // then
    assertThat(androidStudioVersionsPath.resolve("windows_x64.urls.sha256")).exists()
        .hasContent(EXPECTED_ABODY_CHECKSUM);

  }
}
