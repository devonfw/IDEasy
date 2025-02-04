package com.devonfw.tools.ide.url.tool.androidstudio;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for integrations of the {@link AndroidStudioUrlUpdater}.
 */
@WireMockTest
public class AndroidStudioJsonUrlUpdaterTest extends Assertions {

  /**
   * Test resource location
   */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/AndroidStudioJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  private static String androidVersionJson;
  private static String androidVersionWithoutChecksumJson;

  /**
   * Creates an android-version and android-version-without-checksum json file based on the given test resource in a temporary directory according to the http
   * url and port of the {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    androidVersionJson = Files.readString(Path.of(TEST_DATA_ROOT).resolve("android-version.json"));
    androidVersionJson = androidVersionJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());

    androidVersionWithoutChecksumJson = Files.readString(Path.of(TEST_DATA_ROOT).resolve("android-version-without-checksum.json"));
    androidVersionWithoutChecksumJson = androidVersionWithoutChecksumJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link AndroidStudioUrlUpdater} to download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200).withBody(androidVersionJson.getBytes())));

    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock(wmRuntimeInfo);

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
   * Test if {@link AndroidStudioUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200).withBody(androidVersionJson.getBytes())));

    stubFor(get(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(404)));

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
  public void testJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/android-studio-releases-list.*")).willReturn(aResponse().withStatus(200).withBody(androidVersionWithoutChecksumJson.getBytes())));

    stubFor(any(urlMatching("/edgedl/android/studio/ide-zips.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AndroidStudioUrlUpdaterMock updater = new AndroidStudioUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path androidStudioVersionsPath = tempDir.resolve("android-studio").resolve("android-studio").resolve("2023.1.1.2");

    // then
    assertThat(androidStudioVersionsPath.resolve("windows_x64.urls.sha256")).exists().hasContent(EXPECTED_ABODY_CHECKSUM);

  }
}
