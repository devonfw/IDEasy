package com.devonfw.tools.ide.url.tool.python;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

/**
 * {@link WireMockTest} using {@link PythonUrlUpdaterMock}.
 */
@WireMockTest
public class PythonUrlUpdaterTest extends Assertions {

  /** Test resource location */
  private final static String TEST_DATA_ROOT = "src/test/resources/integrationtest/PythonJsonUrlUpdater";
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
    pythonVersionJson = new String(Files.readAllBytes(Path.of(TEST_DATA_ROOT).resolve("python-version.json")), StandardCharsets.UTF_8);
    pythonVersionJson = pythonVersionJson.replaceAll("\\$\\{testbaseurl}", wmRuntimeInfo.getHttpBaseUrl());
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
    stubFor(get(urlMatching("/actions/python-versions/main/.*")).willReturn(aResponse().withStatus(200).withBody(pythonVersionJson.getBytes())));

    stubFor(any(urlMatching("/actions/python-versions/releases/download.*")).willReturn(aResponse().withStatus(200).withBody("aBody")));

    UrlRepository urlRepository = UrlRepository.load(tempDir);
    PythonUrlUpdaterMock pythonUpdaterMock = new PythonUrlUpdaterMock(wmRuntimeInfo);
    pythonUpdaterMock.update(urlRepository);
    Path pythonPath = tempDir.resolve("python").resolve("python").resolve("3.12.0");

    assertThat(pythonPath.resolve("status.json")).exists();
    assertThat(pythonPath.resolve("linux_x64.urls")).exists();
    assertThat(pythonPath.resolve("linux_x64.urls.sha256")).exists();
    assertThat(pythonPath.resolve("mac_arm64.urls")).exists();
    assertThat(pythonPath.resolve("mac_arm64.urls.sha256")).exists();
    assertThat(pythonPath.resolve("windows_x64.urls")).exists();
    assertThat(pythonPath.resolve("windows_x64.urls.sha256")).exists();

  }
}
