package com.devonfw.tools.ide.url.go;

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

@WireMockTest
class GoUrlUpdaterTest extends AbstractUrlUpdaterTest {

  @Test
  void testGoUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // given
    stubFor(get(urlMatching("/repos/golang/go/git/refs/tags")).willReturn(
        aResponse().withStatus(200).withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("GoUrlUpdater").resolve("go-tags.json"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/dl/go[\\d\\.]+\\.(windows|linux|darwin)-(amd64|arm64)\\.(zip|tar\\.gz)")).willReturn(
        aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    GoUrlUpdaterMock updater = new GoUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    // then
    Path goEditionPath = tempDir.resolve("go").resolve("go");
    assertUrlVersionOsX64MacArm(goEditionPath.resolve("1.25.7"));
    assertUrlVersionOsX64MacArm(goEditionPath.resolve("1.25.8"));
  }
}
