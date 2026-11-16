package com.devonfw.tools.ide.url.tool.jq;

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

@WireMockTest
class JqUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link JqUrlUpdater} for the creation of download URLs and checksums for all platform/architecture combinations.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testJqUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/jqlang/jq/releases")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("JqUrlUpdater").resolve("jq-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/jqlang/jq/releases/download/[^/]*/jq.*"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    JqUrlUpdater updater = new JqUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path jqDir = tempDir.resolve("jq").resolve("jq");
    assertUrlVersionOsArch(jqDir.resolve("1.8.2"));
    assertThat(jqDir.resolve("1.8.1")).doesNotExist();
  }
}
