package com.devonfw.tools.ide.url.tool.sonar;

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
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link SonarUrlUpdater}.
 */
@WireMockTest
class SonarUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for SonarUrlUpdater: verifies that update creates expected files for SonarQube versions.
   */
  @Test
  void testSonarUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/SonarSource/sonarqube/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("SonarUrlUpdater").resolve("sonar-tags.json"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/Distribution/sonarqube/sonarqube-.*\\.zip"))
        .willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    SonarUrlUpdaterMock updater = new SonarUrlUpdaterMock(wmRuntimeInfo);
    // act
    updater.update(urlRepository);

    // assert
    Path sonarEditionPath = tempDir.resolve("sonar").resolve("sonar");
    for (String version : new String[] { "10.4.0.87286", "10.5.0.90531" }) {
      Path sonarVersionPath = sonarEditionPath.resolve(version);
      assertUrlVersionAgnostic(sonarVersionPath);
    }
  }
}

