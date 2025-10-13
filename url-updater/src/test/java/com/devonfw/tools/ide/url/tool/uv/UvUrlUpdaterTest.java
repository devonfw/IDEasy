package com.devonfw.tools.ide.url.tool.uv;

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
 * Test of {@link UvUrlUpdater}.
 */
@WireMockTest
public class UvUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Integration test for UvUrlUpdater: verifies that update creates expected files for uv versions.
   */
  @Test
  void testUvUrlUpdaterCreatesDownloadUrlsAndChecksums(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    // arrange
    stubFor(get(urlMatching("/repos/astral-sh/uv/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("UvUrlUpdater").resolve("uv-tags.json"), wmRuntimeInfo))));
    stubFor(
        any(urlMatching("/astral-sh/uv/releases/download/.*/uv-(aarch64|x86_64)-(unknown-linux-gnu|pc-windows-msvc|apple-darwin)\\.(zip|tar\\.gz)")).willReturn(
            aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    UvUrlUpdaterMock updater = new UvUrlUpdaterMock(wmRuntimeInfo);
    // act
    updater.update(urlRepository);

    // assert
    Path uvEditionPath = tempDir.resolve("uv").resolve("uv");
    for (String version : new String[] { "0.8.10", "0.8.4" }) {
      Path uvVersionPath = uvEditionPath.resolve(version);
      assertUrlVersionOsX64MacArm(uvVersionPath);
    }
  }

}
