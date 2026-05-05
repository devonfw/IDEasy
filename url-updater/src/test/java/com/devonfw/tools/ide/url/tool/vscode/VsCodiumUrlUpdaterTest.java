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
 * Test of {@link VsCodiumUrlUpdater}.
 */
@WireMockTest
class VsCodiumUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * verifies that update creates expected files for VSCodium versions.
   */
  @Test
  void testVsCodiumUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // given
    stubFor(get(urlMatching("/repos/VSCodium/vscodium/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(Files.readAllBytes(PATH_INTEGRATION_TEST.resolve("VsCodiumUrlUpdater").resolve("vscodium-tags.json")))));
    stubFor(head(urlMatching("/.*/VSCodium-(linux|darwin|win32)-(x64|arm64)-.*\\.(tar\\.gz|zip)")).willReturn(aResponse().withStatus(200)));
    stubFor(get(urlMatching("/.*/VSCodium-(linux|darwin|win32)-(x64|arm64)-.*\\.(tar\\.gz|zip)")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    VsCodiumUrlUpdaterMock updater = new VsCodiumUrlUpdaterMock(wmRuntimeInfo);
    // when
    updater.update(urlRepository);

    Path vscodium1 = tempDir.resolve("vscode").resolve("vscodium").resolve("1.92.1.24228");
    Path vscodium2 = tempDir.resolve("vscode").resolve("vscodium").resolve("1.116.02821");

    // then
    assertUrlVersionOsArch(vscodium1);
    assertUrlVersionOsArch(vscodium2);
  }
}
