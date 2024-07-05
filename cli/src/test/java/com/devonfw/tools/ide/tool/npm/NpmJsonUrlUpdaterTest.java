package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Test class for integrations of the {@link NpmUrlUpdater}
 */
@WireMockTest(httpPort = 8080)
public class NpmJsonUrlUpdaterTest extends Assertions {

  /**
   * Test resource location
   */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/NpmJsonUrlUpdater";

  /** This is the SHA256 checksum of aBody (a placeholder body which gets returned by WireMock) */
  private static final String EXPECTED_ABODY_CHECKSUM = "de08da1685e537e887fbbe1eb3278fed38aff9da5d112d96115150e8771a0f30";

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link NpmUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("npm-version.json")))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path NpmVersionsPath = tempDir.resolve("npm").resolve("npm").resolve("1.2.32");

    // then
    assertThat(NpmVersionsPath.resolve("status.json")).exists();
    assertThat(NpmVersionsPath.resolve("urls")).exists();
    assertThat(NpmVersionsPath.resolve("urls.sha256")).exists();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link NpmUrlUpdater} for a non-existent version does successfully not
   * create a download folder.
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir)
      throws IOException {

    // given
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("npm-version.json")))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path npmVersionsPath = tempDir.resolve("npm").resolve("npm").resolve("99.99.99");

    // then
    assertThat(npmVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link NpmUrlUpdater} can handle filtering of versions.
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Disabled
  @Test
  public void testNpmJsonUrlUpdaterFilteredVersionCreateVersionFolder(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("npm-version.json")))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path npmVersionsPath = tempDir.resolve("npm").resolve("npm").resolve("2.0.0-beta.0");

    // then
    assertThat(npmVersionsPath).doesNotExist();

  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link NpmUrlUpdater} can handle downloads with missing checksums (generate
   * checksum from download file if no checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterGeneratesChecksum(@TempDir Path tempDir) throws IOException {

    // given
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("npm-version.json")))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock();

    // when
    updater.update(urlRepository);

    Path NpmVersionsPath = tempDir.resolve("npm").resolve("npm").resolve("1.1.25");

    // then
    assertThat(NpmVersionsPath.resolve("urls.sha256")).exists().hasContent(EXPECTED_ABODY_CHECKSUM);

  }
}
