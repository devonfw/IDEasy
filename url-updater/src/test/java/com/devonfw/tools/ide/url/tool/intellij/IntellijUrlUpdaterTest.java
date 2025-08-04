package com.devonfw.tools.ide.url.tool.intellij;

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
 * Test class for integrations of the {@link IntellijUrlUpdater}
 */
@WireMockTest
public class IntellijUrlUpdaterTest extends AbstractUrlUpdaterTest {

  private static String intellijVersionJson;
  private static String intellijVersionWithoutChecksumJson;

  /**
   * Creates an intellij-version and intellij-version-without-checksum json file based on the given test resource in a temporary directory according to the http
   * url and port of the {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException on error.
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    Path testDataPath = PATH_INTEGRATION_TEST.resolve("IntellijUrlUpdater");
    intellijVersionJson = readAndResolve(testDataPath.resolve("intellij-version.json"), wmRuntimeInfo);
    intellijVersionWithoutChecksumJson = readAndResolve(testDataPath.resolve("intellij-version-without-checksum.json"), wmRuntimeInfo);
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link IntellijUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testIntellijJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(intellijVersionJson)));

    stubFor(any(urlMatching("/idea/idea.*\\.tar\\.gz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));
    stubFor(any(urlMatching("/idea/idea.*\\.tar\\.gz\\.sha256")).willReturn(aResponse().withStatus(200).withBody(SHA_256)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path intellijToolPath = tempDir.resolve("intellij");
    for (String edition : List.of("intellij", "ultimate")) {
      Path intellijEditionPath = intellijToolPath.resolve(edition);
      assertThat(intellijEditionPath).exists();
      for (String version : List.of("2023.1.1", "2023.1.2")) {
        Path versionPath = intellijEditionPath.resolve(version);
        assertThat(versionPath).exists();
        assertUrlVersion(versionPath, List.of("linux_x64"));
      }
    }
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

    // arrange
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(intellijVersionJson)));

    stubFor(any(urlMatching("/idea/idea.*\\.tar\\.gz.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.3");

    // assert
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

    // arrange
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200).withBody(intellijVersionWithoutChecksumJson)));

    stubFor(any(urlMatching("/idea/idea.*\\.tar\\.gz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.2");
    assertUrlVersion(intellijVersionsPath, List.of("linux_x64"));
  }
}
