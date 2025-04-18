package com.devonfw.tools.ide.url.tool.intellij;

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
 * Test class for integrations of the {@link IntellijUrlUpdater}
 */
@WireMockTest
public class IntellijJsonUrlUpdaterTest extends Assertions {

  /** Test resource location */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/IntellijJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  private static String intellijVersionJson;
  private static String intellijVersionWithoutChecksumJson;

  /**
   * Creates an intellij-version and intellij-version-without-checksum json file based on the given test resource in a temporary directory according to the http
   * url and port of the {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    Path testDataPath = Path.of(TEST_DATA_ROOT);
    intellijVersionJson = Files.readString(testDataPath.resolve("intellij-version.json"));
    intellijVersionJson = intellijVersionJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());

    intellijVersionWithoutChecksumJson = Files.readString(testDataPath.resolve("intellij-version-without-checksum.json"));
    intellijVersionWithoutChecksumJson = intellijVersionWithoutChecksumJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link IntellijUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testIntellijJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(intellijVersionJson.getBytes())));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    // then
    Path intellijVersionsPath1 = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.1");
    assertThat(intellijVersionsPath1.resolve("status.json")).exists();
    assertThat(intellijVersionsPath1.resolve("linux_x64.urls")).exists();
    assertThat(intellijVersionsPath1.resolve("linux_x64.urls.sha256")).exists();
    Path intellijVersionsPath2 = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.2");
    assertThat(intellijVersionsPath2.resolve("status.json")).exists();
    assertThat(intellijVersionsPath2.resolve("linux_x64.urls")).exists();
    assertThat(intellijVersionsPath2.resolve("linux_x64.urls.sha256")).exists();
    Path intellijultimateVersionsPath1 = tempDir.resolve("intellij").resolve("ultimate").resolve("2023.1.1");
    assertThat(intellijultimateVersionsPath1.resolve("status.json")).exists();
    assertThat(intellijultimateVersionsPath1.resolve("linux_x64.urls")).exists();
    assertThat(intellijultimateVersionsPath1.resolve("linux_x64.urls.sha256")).exists();
    Path intellijultimateVersionsPath2 = tempDir.resolve("intellij").resolve("ultimate").resolve("2023.1.2");
    assertThat(intellijultimateVersionsPath2.resolve("status.json")).exists();
    assertThat(intellijultimateVersionsPath2.resolve("linux_x64.urls")).exists();
    assertThat(intellijultimateVersionsPath2.resolve("linux_x64.urls.sha256")).exists();


  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link IntellijUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no
   * checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testIntellijJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(intellijVersionJson.getBytes())));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.3");

    // then
    assertThat(intellijVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link IntellijUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no
   * checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testIntellijJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(intellijVersionWithoutChecksumJson.getBytes())));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.2");

    // then
    assertThat(intellijVersionsPath.resolve("linux_x64.urls.sha256")).exists().hasContent(EXPECTED_ABODY_CHECKSUM);

  }
}
