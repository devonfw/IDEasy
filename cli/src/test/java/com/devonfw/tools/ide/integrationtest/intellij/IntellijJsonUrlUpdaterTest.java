package com.devonfw.tools.ide.integrationtest.intellij;

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
import com.devonfw.tools.ide.url.updater.intellij.IntellijUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for integrations of the {@link IntellijUrlUpdater}
 */
@WireMockTest(httpPort = 8080)
public class IntellijJsonUrlUpdaterTest extends Assertions {

  /**
   * Test resource location
   */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/IntellijJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link IntellijUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testIntellijJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Paths.get(TEST_DATA_ROOT).resolve("intellij-version.json")))));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.1");

    // then
    assertThat(intellijVersionsPath.resolve("status.json")).exists();
    assertThat(intellijVersionsPath.resolve("linux_x64.urls")).exists();
    assertThat(intellijVersionsPath.resolve("linux_x64.urls.sha256")).exists();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link IntellijUrlUpdater} can handle downloads with missing checksums
   * (generate checksum from download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testIntellijJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir)
      throws IOException {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Paths.get(TEST_DATA_ROOT).resolve("intellij-version.json")))));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(404)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.3");

    // then
    assertThat(intellijVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link IntellijUrlUpdater} can handle downloads with missing checksums
   * (generate checksum from download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testIntellijJsonUrlUpdaterWithMissingChecksumGeneratesChecksum(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/products.*")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Paths.get(TEST_DATA_ROOT).resolve("intellij-version-withoutchecksum.json")))));

    stubFor(any(urlMatching("/idea/idea.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    IntellijUrlUpdaterMock updater = new IntellijUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path intellijVersionsPath = tempDir.resolve("intellij").resolve("intellij").resolve("2023.1.2");

    // then
    assertThat(intellijVersionsPath.resolve("linux_x64.urls.sha256")).exists().hasContent(EXPECTED_ABODY_CHECKSUM);

  }
}
