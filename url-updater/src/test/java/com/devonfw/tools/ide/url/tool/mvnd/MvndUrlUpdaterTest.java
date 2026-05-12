package com.devonfw.tools.ide.url.tool.mvnd;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

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
 * Test of {@link MvndUrlUpdater}
 */
@WireMockTest
@SuppressWarnings("unused")
class MvndUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link MvndUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
   @Test
   void testMvndJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

     // arrange
     stubFor(get(urlMatching("/repos/apache/maven-mvnd/releases")).willReturn(
         aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

     stubFor(any(urlMatching("/maven/mvnd/.*\\.zip")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

     UrlRepository urlRepository = UrlRepository.load(tempDir);
     MvndUrlUpdaterMock updater = new MvndUrlUpdaterMock(wmRuntimeInfo);

     // act
     updater.update(urlRepository);

     // assert
     assertUrlVersionOsX64MacArm(tempDir.resolve("mvnd").resolve("mvnd").resolve("1.0.5"));
   }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link MvndUrlUpdater} can handle filtering of old versions.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
   @Test
   void testMvndJsonUrlUpdaterFilterOldVersions(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

     // arrange
     stubFor(get(urlMatching("/repos/apache/maven-mvnd/releases")).willReturn(
         aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

     stubFor(any(urlMatching("/maven/mvnd/.*\\.zip")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

     UrlRepository urlRepository = UrlRepository.load(tempDir);
     MvndUrlUpdaterMock updater = new MvndUrlUpdaterMock(wmRuntimeInfo);

     // act
     updater.update(urlRepository);

     // assert
     Path mvndOldVersionPath = tempDir.resolve("mvnd").resolve("mvnd").resolve("1.0.0");
     assertThat(mvndOldVersionPath).doesNotExist();
   }

  /**
   * Test if the {@link JsonUrlUpdater} for {@link MvndUrlUpdater} accepts pre-release versions (rc).
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
   @Test
   void testMvndJsonUrlUpdaterAcceptsReleaseCandidate(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

     // arrange
     stubFor(get(urlMatching("/repos/apache/maven-mvnd/releases")).willReturn(
         aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

     stubFor(any(urlMatching("/maven/mvnd/.*\\.zip")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

     UrlRepository urlRepository = UrlRepository.load(tempDir);
     MvndUrlUpdaterMock updater = new MvndUrlUpdaterMock(wmRuntimeInfo);

     // act
     updater.update(urlRepository);

     // assert
     Path mvndRcVersionPath = tempDir.resolve("mvnd").resolve("mvnd").resolve("2.0.0-rc-3");
     assertUrlVersionOsX64MacArm(mvndRcVersionPath);
   }

  private static String getJsonBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    return readAndResolve(PATH_INTEGRATION_TEST.resolve("MvndUrlUpdater").resolve("mvnd-releases.json"), wmRuntimeInfo);
  }
}
