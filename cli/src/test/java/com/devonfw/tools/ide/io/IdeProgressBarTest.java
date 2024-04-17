package com.devonfw.tools.ide.io;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.devonfw.tools.ide.io.FileAccessImpl.DEFAULT_CONTENT_LENGTH;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Test of {@link IdeProgressBar}.
 */
@WireMockTest(httpPort = 8080)
public class IdeProgressBarTest extends AbstractIdeContextTest {

  private static final int MAX_LENGTH = 10_000;

  private static final String TEST_URL = "http://localhost:8080/os/windows_x64_url.tgz";

  /**
   * Tests if a download of a file with a valid content length was displaying an {@link IdeProgressBar} properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarDownloadWithValidContentLength(@TempDir Path tempDir) {

    stubFor(any(urlMatching("/os/.*")).willReturn(
        aResponse().withStatus(200).withBody(new byte[MAX_LENGTH]).withHeader("Content-Length", String.valueOf(MAX_LENGTH))));

    IdeContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    impl.download(TEST_URL, tempDir.resolve("windows_x64_url.tgz"), true);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    assertProgressBar(context, "Downloading", MAX_LENGTH);
  }

  /**
   * Tests if {@link FileAccessImpl#download(String, Path, boolean)} with default value for missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarDownloadWithDefaultValueForMissingContentLength(@TempDir Path tempDir) {

    //arrange
    String taskName = "Downloading";
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[MAX_LENGTH])));
    IdeTestContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    //act
    impl.download(TEST_URL, tempDir.resolve("windows_x64_url.tgz"), true);

    //assert
    checkLogMessageForDefaultContentLength(context, TEST_URL);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(DEFAULT_CONTENT_LENGTH);
  }

  private void checkLogMessageForDefaultContentLength(IdeTestContext context, String source) {

    assertLogMessage(context, IdeLogLevel.WARNING,
        "Content-Length was not provided by download/copy source: " + source + ". Using fallback: Content-Length for the progress bar is set to 10000000.");
  }

  /**
   * Tests if {@link FileAccessImpl#download(String, Path, boolean)} with default value for missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarCopyWithDefaultValueForMissingContentLength(@TempDir Path tempDir) {

    //arrange
    String taskName = "Copying";
    IdeTestContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    String source = Path.of("src/test/resources/__files").resolve("testZip").toString();

    //act
    impl.download(source, tempDir.resolve("windows_x64_url.tgz"), true);

    //assert
    checkLogMessageForDefaultContentLength(context, source);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(DEFAULT_CONTENT_LENGTH);
  }
}
