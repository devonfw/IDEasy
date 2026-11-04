package com.devonfw.tools.ide.url.tool.ruby;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link RubyJdxUrlUpdater}.
 */
@WireMockTest
class RubyJdxUrlUpdaterTest extends AbstractUrlUpdaterTest {

  /**
   * Test of {@link RubyJdxUrlUpdater} for the creation of Linux and macOS download URLs and checksums.
   *
   * @param tempDir path to a temporary directory
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo}
   * @throws IOException if the test fails
   */
  @Test
  void testRubyJdxUrlUpdater(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) throws IOException {

    // arrange
    stubFor(get(urlMatching("/repos/jdx/ruby/releases"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readAndResolve(
                PATH_INTEGRATION_TEST.resolve("RubyJdxUrlUpdater").resolve("ruby-releases.json"),
                wmRuntimeInfo))));

    stubFor(any(urlMatching("/jdx/ruby/releases/download/.*"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    RubyJdxUrlUpdater updater =
        new RubyJdxUrlUpdater(wmRuntimeInfo.getHttpBaseUrl(), wmRuntimeInfo.getHttpBaseUrl());

    // act
    update(updater, urlRepository);

    // assert
    Path rubyVersionDir = tempDir.resolve("ruby").resolve("ruby").resolve("4.0.6-1");

    assertThat(rubyVersionDir.resolve("status.json")).exists();
    assertUrlVersionFile(rubyVersionDir, "linux_x64");
    assertUrlVersionFile(rubyVersionDir, "linux_arm64");
    assertUrlVersionFile(rubyVersionDir, "mac_arm64");

    verify(anyRequestedFor(urlEqualTo(
        "/jdx/ruby/releases/download/4.0.6-1/ruby-4.0.6.x86_64_linux.tar.gz")));

    verify(anyRequestedFor(urlEqualTo(
        "/jdx/ruby/releases/download/4.0.6-1/ruby-4.0.6.arm64_linux.tar.gz")));

    verify(anyRequestedFor(urlEqualTo(
        "/jdx/ruby/releases/download/4.0.6-1/ruby-4.0.6.macos.tar.gz")));
  }
}
