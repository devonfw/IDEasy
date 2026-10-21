package com.devonfw.tools.ide.url.tool.dart;

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
 * Test of {@link DartUrlUpdater}.
 */
@WireMockTest
class DartUrlUpdaterTest extends AbstractUrlUpdaterTest {

  @Test
  void testDartUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    Path testDir = PATH_INTEGRATION_TEST.resolve("DartUrlUpdater");
    stubFor(get(urlMatching("/channels/stable/release/latest/VERSION")).willReturn(
        aResponse().withStatus(200).withBody(readAndResolve(testDir.resolve("VERSION"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/channels/stable/release/[\\d.]+/sdk/dartsdk-(linux|macos|windows)-(x64|arm64)-release\\.zip")).willReturn(
        aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    DartUrlUpdater updater = new DartUrlUpdater(wmRuntimeInfo.getHttpBaseUrl());
    // when
    update(updater, urlRepository);

    // then
    Path dartEditionPath = tempDir.resolve("dart").resolve("dart");
    assertUrlVersionDart(dartEditionPath.resolve("3.13.5"));
  }
}
