package com.devonfw.tools.ide.url.tool.corepack;

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
 * Test class for integrations of the {@link CorepackUrlUpdater}
 */
@WireMockTest
public class CorepackUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link JsonUrlUpdater} for the creation of {@link CorepackUrlUpdater} download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   * @throws IOException test fails
   */
  @Test
  public void testCorepackJsonUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/corepack")).willReturn(
        aResponse().withStatus(200).withBody(getJsonBody(wmRuntimeInfo))));

    stubFor(any(urlMatching("/corepack/-/corepack-[0-9.].*.tgz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    CorepackUrlUpdaterMock updater = new CorepackUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    assertUrlVersionAgnostic(tempDir.resolve("corepack").resolve("corepack").resolve("0.34.0"));
  }

  private static String getJsonBody(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    return readAndResolve(PATH_INTEGRATION_TEST.resolve("CorepackUrlUpdater").resolve("corepack-version.json"), wmRuntimeInfo);
  }
}
