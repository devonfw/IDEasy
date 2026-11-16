package com.devonfw.tools.ide.url.tool.oc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link OcUrlUpdater}.
 */
@WireMockTest
class OcUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Tests that {@link OcUrlUpdater} picks up versions with a multi-digit minor version number (e.g. 4.10.0, released after OpenShift 4.10) and skips
   * release-candidate folders (e.g. 5.0.0-rc.0). Regression test for <a href="https://github.com/devonfw/IDEasy/issues/2714">#2714</a>.
   *
   * @param tempDir path to a temporary directory.
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}.
   */
  @Test
  void testOcUrlUpdaterPicksUpMultiDigitMinorVersions(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlEqualTo("/pub/openshift-v4/clients/ocp/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(
                PATH_INTEGRATION_TEST.resolve("OcUrlUpdater").resolve("ocp-index.html"),
                wmRuntimeInfo))));

    stubFor(any(urlMatching("/pub/openshift-v4/clients/ocp/[0-9.]+/openshift-client-.*"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    OcUrlUpdater updater = new OcUrlUpdater(
        wmRuntimeInfo.getHttpBaseUrl(),
        wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path ocDir = tempDir.resolve("oc").resolve("oc");
    // single-digit minor version still works
    assertUrlVersionOsX64(ocDir.resolve("4.9.59"));
    // multi-digit minor versions (the regression, released after OpenShift 4.10) are now picked up
    assertUrlVersionOsX64(ocDir.resolve("4.10.0"));
    assertUrlVersionOsX64(ocDir.resolve("4.22.19"));
    // release-candidate folders must not be extracted as a bogus stable version
    assertThat(ocDir.resolve("5.0.0")).doesNotExist();
  }
}
