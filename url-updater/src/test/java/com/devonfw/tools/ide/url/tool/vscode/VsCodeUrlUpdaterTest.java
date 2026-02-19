package com.devonfw.tools.ide.url.tool.vscode;

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
 * Test of {@link VsCodeUrlUpdater}.
 */
@WireMockTest
class VsCodeUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * verifies that update creates expected files for VS Code versions.
   */
  @Test
  void testVsCodeUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // given
    stubFor(get(urlMatching("/repos/microsoft/vscode/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(PATH_INTEGRATION_TEST.resolve("VsCodeUrlUpdater").resolve("vscode-tags.json")))));
    stubFor(head(urlMatching("/.*/(linux-x64|darwin|win32-x64-archive)/stable")).willReturn(aResponse().withStatus(200)));
    stubFor(get(urlMatching("/.*/(linux-x64|darwin|win32-x64-archive)/stable")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    VsCodeUrlUpdaterMock updater = new VsCodeUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    Path vscode120 = tempDir.resolve("vscode").resolve("vscode").resolve("1.20.0");
    Path vscode121 = tempDir.resolve("vscode").resolve("vscode").resolve("1.21.0");

    // then
    assertUrlVersionOsX64(vscode120);
    assertUrlVersionOsX64(vscode121);
  }
}

