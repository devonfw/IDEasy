package com.devonfw.tools.ide.io;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
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
   * Tests if {@link FileAccess#download(String, Path)} with missing content length is working properly.
   *
   * @param tempDir temporary directory to use.
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testProgressBarDownloadWithMissingContentLength(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    //arrange
    String taskName = "Downloading";
    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[MAX_LENGTH])));
    IdeTestContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    //act
    String testUrl = wmRuntimeInfo.getHttpBaseUrl() + TEST_URL;
    impl.download(testUrl, tempDir.resolve("windows_x64_url.tgz"));

    //assert
    assertUnknownProgressBar(context, "Downloading", MAX_LENGTH);
    assertThat(context).logAtWarning().hasMessage(
        "Content-Length was not provided by download from " + testUrl);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);

    assertThat(progressBar.getMaxSize()).isEqualTo(-1);
  }

  /**
   * Tests if {@link FileAccess#download(String, Path)} copy process with known file size is working properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarCopyWithKnownFileSize(@TempDir Path tempDir) {

    //arrange
    String taskName = "Copying";
    IdeTestContext context = newContext(tempDir);
    long maxSize = MAX_LENGTH;
    FileAccessTestImpl impl = new FileAccessTestImpl(context);
    try {
      FileUtils.writeByteArrayToFile(new File(tempDir.resolve("tempFile").toAbsolutePath().toString()), new byte[MAX_LENGTH]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String source = tempDir.resolve("tempFile").toString();

    //act
    impl.download(source, tempDir.resolve("windows_x64_url.tgz"));

    //assert
    assertProgressBar(context, "Copying", maxSize);
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    IdeProgressBarTestImpl progressBar = context.getProgressBarMap().get(taskName);
    assertThat(progressBar.getMaxSize()).isEqualTo(maxSize);
  }
}
