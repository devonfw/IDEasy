package com.devonfw.tools.ide.url.tool.tomcat;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
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
 * Test of {@link TomcatUrlUpdater}.
 */
@WireMockTest
class TomcatUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for TomcatUrlUpdater: verifies that update creates expected files for Tomcat versions.
   */
  @Test
  void testTomcatUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // given
    stubFor(get(urlMatching("/repos/apache/tomcat/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(PATH_INTEGRATION_TEST.resolve("TomcatUrlUpdater").resolve("tomcat-tags.json")))));
    stubFor(head(urlMatching("/dist/tomcat/tomcat-.*/v.*/bin/apache-tomcat-.*\\.tar.gz")).willReturn(aResponse().withStatus(200)));
    stubFor(get(urlMatching("/dist/tomcat/tomcat-.*/v.*/bin/apache-tomcat-.*\\.tar.gz")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    TomcatUrlUpdaterMock updater = new TomcatUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    Path tomcat100 = tempDir.resolve("tomcat").resolve("tomcat").resolve("10.0.0");
    Path tomcat101 = tempDir.resolve("tomcat").resolve("tomcat").resolve("10.0.1");

    // then
    assertUrlVersionAgnostic(tomcat100);
    assertUrlVersionAgnostic(tomcat101);
  }
}

