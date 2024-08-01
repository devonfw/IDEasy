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
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link IdeProgressBar}.
 */
@WireMockTest
public class IdeProgressBarTest extends AbstractIdeContextTest {

  /**
   * Tests if a download of a file with a valid content length was displaying an {@link IdeProgressBar} properly.
   *
   * @param tempDir temporary directory to use.
   */
  @Test
  public void testProgressBarDownloadWithValidContentLength(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    int maxLength = 10_000;

    stubFor(any(urlMatching("/os/.*")).willReturn(aResponse().withStatus(200).withBody(new byte[maxLength])
        .withHeader("Content-Length", String.valueOf(maxLength))));

    IdeContext context = newContext(tempDir);
    FileAccess impl = context.getFileAccess();
    impl.download(wmRuntimeInfo.getHttpBaseUrl() + "/os/windows_x64_url.tgz", tempDir.resolve("windows_x64_url.tgz"));
    assertThat(tempDir.resolve("windows_x64_url.tgz")).exists();
    assertProgressBar(context, "Downloading", maxLength);
  }
}
