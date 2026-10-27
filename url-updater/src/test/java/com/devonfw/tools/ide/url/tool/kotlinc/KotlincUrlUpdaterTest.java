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
 * Test of {@link KotlincUrlUpdater}.
 */
@WireMockTest
@SuppressWarnings("unused")
class KotlincUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link KotlincUrlUpdater} for the creation of the kotlin-compiler download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testKotlincCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/JetBrains/kotlin/releases")).willReturn(aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/JetBrains/kotlin/releases/download/v[0-9]+\\.[0-9]+\\.[0-9]+/kotlin-compiler-[0-9]+\\.[0-9]+\\.[0-9]+\\.zip"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    KotlincUrlUpdater updater = new KotlincUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path version2421 = tempDir.resolve("kotlinc").resolve("kotlinc").resolve("2.4.21");
    assertUrlVersionAgnostic(version2421);
    assertThat(version2421.resolve("urls")).exists().content().contains("kotlin-compiler-2.4.21.zip");
  }

  /**
   * Test if the {@link KotlincUrlUpdater} filters out pre-release (beta) versions.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testKotlincFiltersPrerelease(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/JetBrains/kotlin/releases")).willReturn(aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/JetBrains/kotlin/releases/download/v[0-9]+\\.[0-9]+\\.[0-9]+/kotlin-compiler-[0-9]+\\.[0-9]+\\.[0-9]+\\.zip"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    KotlincUrlUpdater updater = new KotlincUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    assertThat(tempDir.resolve("kotlinc").resolve("kotlinc").resolve("2.5.0-Beta2")).doesNotExist();
    assertThat(tempDir.resolve("kotlinc").resolve("kotlinc").resolve("2.4.21")).exists();
  }

  private static String getJsonBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    return readAndResolve(PATH_INTEGRATION_TEST.resolve("KotlincUrlUpdater").resolve("kotlin-releases.json"), wmRuntimeInfo);
  }
}
