package com.devonfw.tools.ide.url.tool.az;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link AzureUrlUpdater}.
 */
@WireMockTest
class AzureUrlUpdaterTest extends Assertions {

  private static final String TEST_DATA_ROOT = "src/test/resources/integrationtest/AzureUrlUpdater";

  /**
   * Integration test for AzureUrlUpdater: verifies that update creates expected files for Azure CLI versions.
   */
  @Test
  void testAzureUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // given
    stubFor(get(urlMatching("/azure-cli/tags")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("azure-cli-tags.json")))));
    stubFor(head(urlMatching("/msi/azure-cli-.*\\.msi")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    AzureUrlUpdaterMock updater = new AzureUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    Path azCli2180 = tempDir.resolve("az").resolve("az").resolve("2.18.0");
    Path azCli2200 = tempDir.resolve("az").resolve("az").resolve("2.20.0");

    // then
    assertThat(azCli2180.resolve("status.json")).exists();
    assertThat(azCli2180.resolve("windows_x64.urls")).exists();
    assertThat(azCli2180.resolve("windows_x64.urls.sha256")).exists();
    assertThat(azCli2200.resolve("status.json")).exists();
    assertThat(azCli2200.resolve("windows_x64.urls")).exists();
    assertThat(azCli2200.resolve("windows_x64.urls.sha256")).exists();
  }
}
