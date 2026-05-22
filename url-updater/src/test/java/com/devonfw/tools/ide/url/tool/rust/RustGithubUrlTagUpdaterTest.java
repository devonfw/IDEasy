package com.devonfw.tools.ide.url.tool.rust;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link RustUrlUpdater}.
 */
@WireMockTest
class RustUrlUpdaterTest extends AbstractUrlUpdaterTest {

  @Test
  void testRustGithubUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // arrange
    stubFor(get(urlMatching("/repos/rust-lang/rustup/git/refs/tags")).willReturn(aResponse().withStatus(200)
        .withBody(readAndResolve(PATH_INTEGRATION_TEST.resolve("RustUrlUpdater").resolve("github-tags.json"), wmRuntimeInfo))));
    stubFor(any(urlMatching("/rustup\\.sh")).willReturn(aResponse().withStatus(200).withHeader("content-type", "text/plain").withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    RustUrlUpdaterMock updater = new RustUrlUpdaterMock(wmRuntimeInfo);

    // act
    updater.update(urlRepository);

    // assert
    Path rustEditionDir = tempDir.resolve("rust").resolve("rust");
    assertUrlVersionAgnostic(rustEditionDir.resolve("1.79.0"));
    assertUrlVersionAgnostic(rustEditionDir.resolve("1.80.1"));
    assertThat(rustEditionDir.resolve("release-0.7")).doesNotExist();
  }
}
