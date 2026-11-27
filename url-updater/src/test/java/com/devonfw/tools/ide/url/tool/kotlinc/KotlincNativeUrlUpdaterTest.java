package com.devonfw.tools.ide.url.tool.kotlinc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link KotlincNativeUrlUpdater}.
 */
@WireMockTest
@SuppressWarnings("unused")
class KotlincNativeUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /** The version-agnostic path of a kotlin-native-prebuilt release asset (all supported OS/arch combinations). */
  private static final String PREBUILT_DOWNLOAD_SUFFIX =
      "kotlin-native-prebuilt-[0-9]+\\.[0-9]+\\.[0-9]+-(windows-x86_64\\.zip|linux-x86_64\\.tar\\.gz|macos-x86_64\\.tar\\.gz|macos-aarch64\\.tar\\.gz)";

  /**
   * Test of {@link KotlincNativeUrlUpdater} for the creation of the kotlin-native-prebuilt download URLs and checksums (including the Kotlin 2.x naming).
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testKotlincNativeCreatesPrebuiltDownloadUrls(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/JetBrains/kotlin/releases")).willReturn(aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/JetBrains/kotlin/releases/download/v[0-9]+\\.[0-9]+\\.[0-9]+/" + PREBUILT_DOWNLOAD_SUFFIX))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    KotlincNativeUrlUpdater updater = new KotlincNativeUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path version2421 = tempDir.resolve("kotlinc-native").resolve("kotlinc-native").resolve("2.4.21");
    assertUrlVersionOsX64MacArm(version2421);
    assertThat(version2421.resolve("windows_x64.urls")).exists().content()
        .contains("kotlin-native-prebuilt-2.4.21-windows-x86_64.zip");
  }

  /**
   * Test if the {@link KotlincNativeUrlUpdater} filters out pre-release (beta) versions.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testKotlincNativeFiltersPrerelease(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/JetBrains/kotlin/releases")).willReturn(aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/JetBrains/kotlin/releases/download/v[12]\\.[0-9]+\\.[0-9]+/" + PREBUILT_DOWNLOAD_SUFFIX))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    KotlincNativeUrlUpdater updater = new KotlincNativeUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    assertThat(tempDir.resolve("kotlinc-native").resolve("kotlinc-native").resolve("2.5.0-Beta2")).doesNotExist();
    assertThat(tempDir.resolve("kotlinc-native").resolve("kotlinc-native").resolve("2.4.21")).exists();
  }

  private static String getJsonBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    return readAndResolve(PATH_INTEGRATION_TEST.resolve("KotlincNativeUrlUpdater").resolve("kotlin-releases.json"), wmRuntimeInfo);
  }
}
