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
public class IdeProgressBarTest extends AbstractIdeContextTest {

  private static final int MAX_LENGTH = 10_000;

  private static final String TEST_URL = "/os/windows_x64_url.tgz";

  /**
   * Tests if a download of a file with a valid content length was displaying an {@link IdeProgressBar} properly.
   *
   * @param tempDir temporary directory to use.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testProgressBarDownloadWithValidContentLength(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    stubFor(any(urlMatching("/os/.*")).willReturn(
        aResponse().withStatus(200).withBody(new byte[MAX_LENGTH]).withHeader("Content-Length", String.valueOf(MAX_LENGTH))));

    IdeContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    impl.download(wmRuntimeInfo.getHttpBaseUrl() + TEST_URL, tempDir.resolve("windows_x64_url.tgz"));
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    assertProgressBar(context, "Downloading", MAX_LENGTH);
  }

  /**
   * Tests if {@link FileAccess#download(String, Path)} with default value for missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testProgressBarDownloadWithDefaultValueForMissingContentLength(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    //arrange
    String taskName = "Downloading";
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[MAX_LENGTH])));
    IdeTestContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    //act
    String testUrl = wmRuntimeInfo.getHttpBaseUrl() + TEST_URL;
    impl.download(testUrl, tempDir.resolve("windows_x64_url.tgz"));

    //assert
    assertProgressBar(context, "Downloading", MAX_LENGTH);
    checkLogMessageForDefaultContentLength(context, testUrl);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);

    assertThat(progressBar.getMaxSize()).isEqualTo(-1);
  }

  private void checkLogMessageForDefaultContentLength(IdeTestContext context, String source) {

    assertThat(context).logAtWarning().hasMessage(
        "Content-Length was not provided by download/copy source: " + source + ".");
  }

  /**
   * Tests if {@link FileAccess#download(String, Path)} with default value for missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarCopyWithDefaultValueForMissingContentLength(@TempDir Path tempDir) {

    //arrange
    String taskName = "Copying";
    IdeTestContext context = newContext(tempDir);

    FileAccessTestImpl impl = new FileAccessTestImpl(context);
    String source = Path.of("src/test/resources/__files").resolve("testZip").toString();

    //act
    impl.download(source, tempDir.resolve("windows_x64_url.tgz"));

    //assert
    checkLogMessageForDefaultContentLength(context, source);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(-1);
  }
}
