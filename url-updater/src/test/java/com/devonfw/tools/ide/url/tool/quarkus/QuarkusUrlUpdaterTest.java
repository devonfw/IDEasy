package com.devonfw.tools.ide.url.tool.quarkus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link QuarkusUrlUpdater}.
 */
@WireMockTest
class QuarkusUrlUpdaterTest extends AbstractUrlUpdaterTest {

  private static final String TEST_DATA_ROOT = "src/test/resources/integrationtest/QuarkusUrlUpdater";

  /**
   * Integration test for QuarkusUrlUpdater: verifies that update creates expected files for Quarkus versions.
   */
  @Test
  void testQuarkusUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/quarkusio/quarkus/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("quarkus-tags.json")))));
    stubFor(
        any(urlMatching("/quarkusio/quarkus/releases/download/.*/quarkus-cli-.*\\.(zip|tar\\.gz)")).willReturn(
            aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    QuarkusUrlUpdaterMock updater = new QuarkusUrlUpdaterMock(wmRuntimeInfo);
    // act
    updater.update(urlRepository);

    // assert
    Path quarkusEditionPath = tempDir.resolve("quarkus").resolve("quarkus");
    for (String version : new String[] { "2.10.0", "2.11.0" }) {
      Path quarkusVersionPath = quarkusEditionPath.resolve(version);
      assertUrlVersionAgnostic(quarkusVersionPath);
    }
  }
}

