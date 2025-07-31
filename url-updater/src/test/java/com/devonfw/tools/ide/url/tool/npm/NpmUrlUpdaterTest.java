package com.devonfw.tools.ide.url.tool.npm;

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
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test class for integrations of the {@link NpmUrlUpdater}
 */
@WireMockTest
public class NpmUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link NpmUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    assertUrlVersionAgnostic(tempDir.resolve("npm").resolve("npm").resolve("1.2.32"));
  }

  private static String getJsonBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    return readAndResolve(PATH_INTEGRATION_TEST.resolve("NpmUrlUpdater").resolve("npm-version.json"), wmRuntimeInfo);
  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link NpmUrlUpdater} for a non-existent version does successfully not create a download folder.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterWithMissingDownloadsDoesNotCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo)
      throws IOException {

    // arrange
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    assertThat(tempDir.resolve("npm").resolve("npm").resolve("99.99.99")).doesNotExist();
  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link NpmUrlUpdater} can handle filtering of versions.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterFilteredVersionCreateVersionFolder(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path npmVersionsPath = tempDir.resolve("npm").resolve("npm").resolve("2.0.0-beta.0");
    assertThat(npmVersionsPath).doesNotExist();
  }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link NpmUrlUpdater} can handle downloads with missing checksums (generate checksum from download file if no
   * checksum was provided)
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testNpmJsonUrlUpdaterGeneratesChecksum(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/npm")).willReturn(
        aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/npm/-/npm-[1-9.]*.tgz")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NpmUrlUpdaterMock updater = new NpmUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    assertUrlVersionAgnostic(tempDir.resolve("npm").resolve("npm").resolve("1.1.25"));
  }
}
