package com.devonfw.tools.ide.url.tool.androidstudio;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link AndroidStudioUrlUpdater}.
 */
@WireMockTest
class AndroidStudioUrlUpdaterTest extends AbstractUrlUpdaterTest {

  private static String androidVersionJson;
  private static String androidVersionWithoutChecksumJson;

  /**
   * Creates an android-version and android-version-without-checksum json file based on the given test resource in a temporary directory according to the http
   * url and port of the {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException on error.
   */
  @BeforeAll
  static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    Path testDataPath = PATH_INTEGRATION_TEST.resolve("AndroidStudioUrlUpdater");
    androidVersionJson = readAndResolve(testDataPath.resolve("android-version.json"), wmRuntimeInfo);
    androidVersionWithoutChecksumJson = readAndResolve(testDataPath.resolve("android-version-without-checksum.json"), wmRuntimeInfo);
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link AndroidStudioUrlUpdater} to download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200).withBody(androidVersionJson)));
    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");
    assertUrlVersionOsX64MacArm(androidStudioVersionsPath);
  }

  /**
   * Test if {@link AndroidStudioUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200).withBody(androidVersionJson)));
    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");

    // then
    assertThat(androidStudioVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link AndroidStudioUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if
   * no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200).withBody(androidVersionWithoutChecksumJson)));
    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");
    assertUrlVersion(androidStudioVersionsPath, List.of("windows_x64"));

  }
}
