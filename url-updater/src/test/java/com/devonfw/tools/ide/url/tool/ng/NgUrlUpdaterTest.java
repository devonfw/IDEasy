package com.devonfw.tools.ide.url.tool.ng;

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
 * Test of {@link NgUrlUpdater}
 */
@WireMockTest
class NgUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link NgUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  void testNgJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/@angular/cli")).willReturn(
        aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/@angular/cli/-/cli-[1-9.].*.tgz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    NgUrlUpdaterMock updater = new NgUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    assertUrlVersionAgnostic(tempDir.resolve("ng").resolve("ng").resolve("20.1.6"));
  }

  private static String getJsonBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    return readAndResolve(PATH_INTEGRATION_TEST.resolve("NgUrlUpdater").resolve("ng-version.json"), wmRuntimeInfo);
  }
}
