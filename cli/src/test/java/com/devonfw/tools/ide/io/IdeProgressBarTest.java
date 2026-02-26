package com.devonfw.tools.ide.io;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link IdeProgressBar}.
 */
@WireMockTest
class IdeProgressBarTest extends AbstractIdeContextTest {

  private static final int MAX_LENGTH = 10_000;

  private static final String TEST_URL = "/os/windows_x64_url.tgz";

  @TempDir
  Path tempDir;

  /**
   * Tests if a download of a file with a valid content length was displaying an {@link IdeProgressBar} properly.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testProgressBarDownloadWithValidContentLength(WireMockRuntimeInfo wmRuntimeInfo) {

    stubFor(any(urlMatching("/os/.*")).willReturn(
        aResponse().withStatus(200).withBody(new byte[MAX_LENGTH]).withHeader("Content-Length", String.valueOf(MAX_LENGTH))));

    IdeContext context = newContext(this.tempDir);
    FileAccess impl = context.getFileAccess();
    impl.download(wmRuntimeInfo.getHttpBaseUrl() + TEST_URL, this.tempDir.resolve("windows_x64_url.tgz"));
    assertThat(this.tempDir.resolve("windows_x64_url.tgz")).exists();
    assertProgressBar(context, "Downloading", MAX_LENGTH);
  }

  /**
   * Tests if {@link FileAccess#download(String, Path)} with missing content length is working properly.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  void testProgressBarDownloadWithMissingContentLength(WireMockRuntimeInfo wmRuntimeInfo) {

    //arrange
    String taskName = "Downloading";
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[MAX_LENGTH])));
    IdeTestContext context = newContext(this.tempDir);
    FileAccess impl = context.getFileAccess();
    //act
    String testUrl = wmRuntimeInfo.getHttpBaseUrl() + TEST_URL;
    impl.download(testUrl, this.tempDir.resolve("windows_x64_url.tgz"));

    //assert
    assertUnknownProgressBar(context, "Downloading", MAX_LENGTH);
    assertThat(context).logAtWarning().hasMessage(
        "Content-Length was not provided by download from " + testUrl);
    assertThat(this.tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);

    assertThat(progressBar.getMaxSize()).isEqualTo(-1);
  }

}
