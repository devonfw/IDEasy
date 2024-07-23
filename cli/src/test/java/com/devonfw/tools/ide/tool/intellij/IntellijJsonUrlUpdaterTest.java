package com.devonfw.tools.ide.tool.intellij;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

  /** Temporary directory for the version json file */
  @TempDir
  static Path tempVersionFilePath;

  /**
   * Creates an intellij-version and intellij-version-without-checksum json file based on the given test resource in a temporary directory according to the http
   * url and port of the {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException
   */
  @BeforeAll
  public static void setupTestVersionFile(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    assertThat(Files.isDirectory(tempVersionFilePath)).isTrue();
    String content = new String(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("intellij-version.json")), StandardCharsets.UTF_8);
    content = content.replaceAll("\\$\\{testbaseurl\\}", wmRuntimeInfo.getHttpBaseUrl());
    Files.write(tempVersionFilePath.resolve("intellij-version.json"), content.getBytes(StandardCharsets.UTF_8));

    content = new String(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("intellij-version-without-checksum.json")), StandardCharsets.UTF_8);
    content = content.replaceAll("\\$\\{testbaseurl\\}", wmRuntimeInfo.getHttpBaseUrl());
    Files.write(tempVersionFilePath.resolve("intellij-version-without-checksum.json"), content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link IntellijUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException test fails
   */
  @Test
  public void testIntellijJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(tempVersionFilePath.resolve("intellij-version.json")))));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock(wmRuntimeInfo);

    // when
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.1");

    // then
    assertThat(intellijVersionsPath.resolve("status.json")).exists();
    assertThat(intellijVersionsPath.resolve("linux_x64.urls")).exists();
    assertThat(intellijVersionsPath.resolve("linux_x64.urls.sha256")).exists();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link IntellijUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no
   * checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException test fails
   */
  @Test
  public void testIntellijJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo)
      throws IOException {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(tempVersionFilePath.resolve("intellij-version.json")))));

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
   * @throws IOException test fails
   */
  @Test
  public void testIntellijJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(tempVersionFilePath.resolve("intellij-version-without-checksum.json")))));

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
