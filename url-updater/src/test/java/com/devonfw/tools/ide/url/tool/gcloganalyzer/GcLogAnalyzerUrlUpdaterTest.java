package com.devonfw.tools.ide.url.tool.gcloganalyzer;


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
 * Test of {@link GcLogAnalyzerUrlUpdater}
 */
@WireMockTest
public class GcLogAnalyzerUrlUpdaterTest extends AbstractUrlUpdaterTest {


  @Test
  void testGcLogAnalyzerUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/gc-log-analyzer/release-notes"))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/html")
            .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("GcLogAnalyzerUrlUpdater").resolve("index.html"), wmRuntimeInfo))));

    stubFor(any(urlMatching("/gcla/26\\.04\\.0/GCLogAnalyzer-26\\.04\\.0-ca\\.zip"))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/zip").withBody(DOWNLOAD_CONTENT)));

    stubFor(any(urlMatching("/gcla/26\\.03\\.0/GCLogAnalyzer-26\\.03\\.0-ca\\.zip"))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/zip").withBody(DOWNLOAD_CONTENT)));

    stubFor(any(urlMatching("/gcla/24\\.10\\.0/GCLogAnalyzer-24\\.10\\.0-ca\\.zip"))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/zip").withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    GcLogAnalyzerUrlUpdater updater = new GcLogAnalyzerUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());

    // act
    updater.update(urlRepository);

    // assert
    Path gclaEditionDir = tempDir.resolve("gcloganalyzer").resolve("gcloganalyzer");

    assertUrlVersionAgnostic(gclaEditionDir.resolve("26.04.0"));
    assertUrlVersionAgnostic(gclaEditionDir.resolve("26.03.0"));
    assertUrlVersionAgnostic(gclaEditionDir.resolve("24.10.0.0"));

    assertThat(gclaEditionDir.resolve("24.10.0.0").resolve("urls"))
        .content()
        .contains(wmRuntimeInfo.getHttpBaseUrl() + "/gcla/24.10.0/GCLogAnalyzer-24.10.0-ca.zip");
  }
}

