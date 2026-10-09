package com.devonfw.tools.ide.url.tool.kubelinter;

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
class KubeLinterUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link KubeLinterUrlUpdater} for the creation of download URLs and checksums for all platform/architecture combinations.
   *
   * @param tempDir Path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testKubeLinterUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/stackrox/kube-linter/releases")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("KubeLinterUrlUpdater").resolve("kube-linter-releases.json"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/stackrox/kube-linter/releases/download/v0\\.8\\.3/kube-linter.*"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    KubeLinterUrlUpdater updater = new KubeLinterUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path kubeLinterDir = tempDir.resolve("kube-linter").resolve("kube-linter");
    assertUrlVersionOsArch(kubeLinterDir.resolve("0.8.3"));
  }
}
