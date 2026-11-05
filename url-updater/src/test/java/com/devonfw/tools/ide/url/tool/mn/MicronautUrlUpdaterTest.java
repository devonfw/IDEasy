package com.devonfw.tools.ide.url.tool.mn;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link MicronautUrlUpdater}.
 */
@WireMockTest
class MicronautUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link MicronautUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testMicronautUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/micronaut-projects/micronaut-starter/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("MicronautUrlUpdater").resolve("micronaut-tags.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/micronaut-projects/micronaut-starter/releases/download/.*/micronaut-cli-.*\\.zip"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    MicronautUrlUpdater updater = new MicronautUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path mnDir = tempDir.resolve("mn").resolve("mn");
    assertUrlVersionAgnostic(mnDir.resolve("5.2.1"));
    assertUrlVersionAgnostic(mnDir.resolve("3.0.0"));
    assertThat(mnDir.resolve("2.5.0")).doesNotExist();
  }
}
