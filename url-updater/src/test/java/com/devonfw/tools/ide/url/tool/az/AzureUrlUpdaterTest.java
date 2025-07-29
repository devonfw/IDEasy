package com.devonfw.tools.ide.url.tool.az;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link AzureUrlUpdater}.
 */
@WireMockTest
class AzureUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for AzureUrlUpdater: verifies that update creates expected files for Azure CLI versions.
   */
  @Test
  void testAzureUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/Azure/azure-cli/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("AzureUrlUpdater").resolve("azure-cli-tags.json"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/msi/azure-cli-.*\\.msi")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AzureUrlUpdaterMock updater = new AzureUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path azEditionDir = tempDir.resolve("az").resolve("az");
    assertUrlVersion(azEditionDir.resolve("2.18.0"), List.of("windows_x64"));
    assertUrlVersion(azEditionDir.resolve("2.20.0"), List.of("windows_x64"));
  }
}
