package com.devonfw.tools.ide.url.tool.ruby;

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
 * Test of {@link RubyUrlUpdater}.
 */
@WireMockTest
class RubyUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link RubyUrlUpdater} for the creation of download URLs and checksums.
   *
   * @param tempDir path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}
   * @throws IOException if the test fails
   */
  @Test
  void testRubyUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/oneclick/rubyinstaller2/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(
                PATH_INTEGRATION_TEST.resolve("RubyUrlUpdater").resolve("ruby-releases.json"),
                wmRuntimeInfo))));

    stubFor(any(urlMatching("/oneclick/rubyinstaller2/releases/download/.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    RubyUrlUpdater updater = new RubyUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    updater.update(urlRepository);

    // assert
    Path rubyVersionDir = tempDir.resolve("ruby").resolve("ruby").resolve("2.5.3-1");
    assertThat(rubyVersionDir.resolve("status.json")).exists();
    assertUrlVersionFile(rubyVersionDir, "windows_x64");

    Path oldRubyVersionDir = tempDir.resolve("ruby").resolve("ruby").resolve("2.4.0-7");
    assertThat(oldRubyVersionDir).doesNotExist();

  }
}
