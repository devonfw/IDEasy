package com.devonfw.tools.ide.io;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.devonfw.tools.ide.io.FileAccessImpl.defaultContentLength;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link IdeProgressBar}.
 */
@WireMockTest(httpPort = 8080)
public class IdeProgressBarTest extends AbstractIdeContextTest {

  private final static int maxLength = 10_000;

  /**
   * Tests if a download of a file with a valid content length was displaying an {@link IdeProgressBar} properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarDownloadWithValidContentLength(@TempDir Path tempDir) {

    stubFor(any(urlMatching("/os/.*")).willReturn(
        aResponse().withStatus(200).withBody(new byte[maxLength]).withHeader("Content-Length", String.valueOf(maxLength))));

    IdeContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    impl.download("http://localhost:8080/os/windows_x64_url.tgz", tempDir.resolve("windows_x64_url.tgz"));
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    assertProgressBar(context, "Downloading", maxLength);
  }

  /**
   * Tests if {@link FileAccessImpl#download(String, Path)} with default value for missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarDownloadWithDefaultValueForMissingContentLength(@TempDir Path tempDir) {

    //arrange
    String taskName = "Downloading";
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[maxLength])));
    IdeTestContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();

    //act
    impl.download("http://localhost:8080/os/windows_x64_url.tgz", tempDir.resolve("windows_x64_url.tgz"));

    //assert
    checkLogMessageForDefaultContentLength(context);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(defaultContentLength);
  }

  private void checkLogMessageForDefaultContentLength(IdeTestContext context) {

    String expectedMessage =
        "Content-Length was not provided by download/copy source. Using fallback: Content-Length for the progress bar is set to " + defaultContentLength + ".";
    assertLogMessage(context, IdeLogLevel.WARNING,
        "Content-Length was not provided by download/copy source. Using fallback: Content-Length for the progress bar is set to 10000000.");
  }

  /**
   * Tests if {@link FileAccessImpl#download(String, Path)} with default value for missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarCopyWithDefaultValueForMissingContentLength(@TempDir Path tempDir) {

    //arrange
    String taskName = "Copying";
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[maxLength])));
    Path pathStub = mock(Path.class);
    when(pathStub.toFile().length()).thenReturn(0L);

    IdeTestContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    String source = Path.of("src/test/resources/__files").resolve("testZip").toString();

    //act
    impl.download(source, tempDir.resolve("windows_x64_url.tgz"));

    //assert
    checkLogMessageForDefaultContentLength(context);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(defaultContentLength);
  }
}
