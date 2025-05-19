package com.devonfw.tools.ide.url.tool.pycharm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
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
 * Test class for integrations of the {@link PycharmUrlUpdater}
 */
@WireMockTest
public class PycharmJsonUrlUpdaterTest extends Assertions {

  /** Test resource location */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/PycharmJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

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
    Path testDataPath = Path.of(TEST_DATA_ROOT);
    pycharmVersionJson = Files.readString(testDataPath.resolve("pycharm-version.json"));
    pycharmVersionJson = pycharmVersionJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());

    pycharmVersionWithoutChecksumJson = Files.readString(testDataPath.resolve("pycharm-version-without-checksum.json"));
    pycharmVersionWithoutChecksumJson = pycharmVersionWithoutChecksumJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link PycharmUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testPycharmJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(any(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(pycharmVersionJson.getBytes())));

    stubFor(any(urlMatching("/python/pycharm.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PycharmUrlUpdaterMock updater = new PycharmUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    // then
    Path pycharmVersionsPath1 = tempDir.resolve("pycharm").resolve("pycharm").resolve("2024.3.5");
    assertThat(pycharmVersionsPath1.resolve("status.json")).exists();
    assertThat(pycharmVersionsPath1.resolve("linux_x64.urls")).exists();
    assertThat(pycharmVersionsPath1.resolve("linux_x64.urls.sha256")).exists();
    Path pycharmVersionsPath2 = tempDir.resolve("pycharm").resolve("pycharm").resolve("2024.3.4");
    assertThat(pycharmVersionsPath2.resolve("status.json")).exists();
    assertThat(pycharmVersionsPath2.resolve("linux_x64.urls")).exists();
    assertThat(pycharmVersionsPath2.resolve("linux_x64.urls.sha256")).exists();
    Path pycharmProfessionalVersionsPath1 = tempDir.resolve("pycharm").resolve("professional").resolve("2024.3.5");
    assertThat(pycharmProfessionalVersionsPath1.resolve("status.json")).exists();
    assertThat(pycharmProfessionalVersionsPath1.resolve("linux_x64.urls")).exists();
    assertThat(pycharmProfessionalVersionsPath1.resolve("linux_x64.urls.sha256")).exists();
    Path pycharmProfessionalVersionsPath2 = tempDir.resolve("pycharm").resolve("professional").resolve("2024.3.4");
    assertThat(pycharmProfessionalVersionsPath2.resolve("status.json")).exists();
    assertThat(pycharmProfessionalVersionsPath2.resolve("linux_x64.urls")).exists();
    assertThat(pycharmProfessionalVersionsPath2.resolve("linux_x64.urls.sha256")).exists();


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
    stubFor(any(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(pycharmVersionJson.getBytes())));

    stubFor(any(urlMatching("/python/pycharm.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PycharmUrlUpdaterMock updater = new PycharmUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path pycharmVersionsPath = tempDir.resolve("pycharm").resolve("pycharm").resolve("2024.3.5");

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

    // given
    stubFor(any(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(pycharmVersionWithoutChecksumJson.getBytes())));

    stubFor(any(urlMatching("/python/pycharm.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PycharmUrlUpdaterMock updater = new PycharmUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path pycharmVersionsPath = tempDir.resolve("pycharm").resolve("pycharm").resolve("2024.3.5");

    // then
    assertThat(pycharmVersionsPath.resolve("linux_x64.urls.sha256")).exists().hasContent(EXPECTED_ABODY_CHECKSUM);

  }
}
