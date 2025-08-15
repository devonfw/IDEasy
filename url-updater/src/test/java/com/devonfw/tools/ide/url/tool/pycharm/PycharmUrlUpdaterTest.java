package com.devonfw.tools.ide.url.tool.pycharm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
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
 * Test class for integrations of the {@link PycharmUrlUpdater}
 */
@WireMockTest
public class PycharmUrlUpdaterTest extends AbstractUrlUpdaterTest {

  private static final String VERSION_2024_3_4 = "2024.3.4";
  private static final String VERSION_2024_3_5 = "2024.3.5";

  private static String pycharmVersionJson;
  private static String pycharmVersionWithoutChecksumJson;

  /**
   * Creates a pycharm-version and pycharm-version-without-checksum json file based on the given test resource in a temporary directory according to the http
   * url and port of the {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException test fails.
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    Path testDataPath = PATH_INTEGRATION_TEST.resolve("PycharmUrlUpdater");
    pycharmVersionJson = readAndResolve(testDataPath.resolve("pycharm-version.json"), wmRuntimeInfo);
    pycharmVersionWithoutChecksumJson = readAndResolve(testDataPath.resolve("pycharm-version-without-checksum.json"), wmRuntimeInfo);
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link PycharmUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testPycharmJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(any(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(pycharmVersionJson)));

    stubFor(any(urlMatching("/python/pycharm.*.tar.gz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));
    stubFor(any(urlMatching("/python/pycharm.*.tar.gz.sha256")).willReturn(aResponse().withStatus(200).withBody(SHA_256)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PycharmUrlUpdaterMock updater = new PycharmUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path pycharmToolDir = tempDir.resolve("pycharm");
    for (String edition : List.of("pycharm", "professional")) {
      Path editionDir = pycharmToolDir.resolve(edition);
      assertThat(editionDir).exists();
      for (String version : List.of(VERSION_2024_3_4, VERSION_2024_3_5)) {
        Path versionDir = editionDir.resolve(version);
        assertThat(versionDir).exists();
        assertUrlVersionFile(versionDir, "linux_x64");
      }
    }
  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link PycharmUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no
   * checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testPycharmJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(any(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(pycharmVersionJson)));

    stubFor(any(urlMatching("/python/pycharm.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PycharmUrlUpdaterMock updater = new PycharmUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path pycharmVersionsPath = tempDir.resolve("pycharm").resolve("pycharm").resolve(VERSION_2024_3_5);

    // then
    assertThat(pycharmVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link PycharmUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no
   * checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testPycharmJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(any(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(pycharmVersionWithoutChecksumJson)));

    stubFor(any(urlMatching("/python/pycharm.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PycharmUrlUpdaterMock updater = new PycharmUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    Path pycharmVersionsPath = tempDir.resolve("pycharm").resolve("pycharm").resolve(VERSION_2024_3_5);

    // assert
    assertUrlVersionFile(pycharmVersionsPath, "linux_x64");
  }
}
