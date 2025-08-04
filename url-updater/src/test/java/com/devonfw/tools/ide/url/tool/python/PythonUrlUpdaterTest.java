package com.devonfw.tools.ide.url.tool.python;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdaterTest;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * Test of {@link PythonUrlUpdater}.
 */
@WireMockTest
public class PythonUrlUpdaterTest extends AbstractUrlUpdaterTest {

  private static String pythonVersionJson;

  /**
   * Creates a python-version json file based on the given test resource in a temporary directory according to the http url and port of the
   * {@link WireMockRuntimeInfo}.
   *
   * @param wmRuntimeInfo wireMock server on a random port
   * @throws IOException
   */
  @BeforeAll
  public static void setupTestVersionJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
    //preparing test data with dynamic port
    pythonVersionJson = readAndResolve(PATH_INTEGRATION_TEST.resolve("PythonUrlUpdater").resolve("python-version.json"), wmRuntimeInfo);
  }


  /**
   * Test Python JsonUrlUpdater
   *
   * @param tempDir Path to a temporary directory
   * @param wmRuntimeInfo wireMock server on a random port
   */
  @Test
  public void testPythonURl(@TempDir Path tempDir, WireMockRuntimeInfo wmRuntimeInfo) {

    // given
    stubFor(get(urlMatching("/actions/python-versions/main/.*")).willReturn(aResponse().withStatus(200).withBody(pythonVersionJson)));

    stubFor(any(urlMatching("/actions/python-versions/releases/download.*")).willReturn(aResponse().withStatus(200).withBody(DOWNLOAD_CONTENT)));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PythonUrlUpdaterMock pythonUpdaterMock = new PythonUrlUpdaterMock(wmRuntimeInfo);
    pythonUpdaterMock.update(urlRepository);
    Path pythonPath = tempDir.resolve("python").resolve("python").resolve("3.12.0");

    assertUrlVersionOsDefArch(pythonPath);
  }
}
