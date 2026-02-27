package com.devonfw.tools.ide.url.tool.docker;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link DockerDesktopUrlUpdater} to allow integration testing with wiremock.
 */
public class DockerDesktopUrlUpdaterMock extends DockerDesktopUrlUpdater {

  private final String baseUrl;

  /**
   * The constructor.
   *
   * @param wireMockRuntimeInfo wireMock server on a random port
   */
  public DockerDesktopUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  public String getDownloadBaseUrl() {

    return baseUrl;
  }

  @Override
  protected String getVersionBaseUrl() {

    return this.baseUrl;
  }

}

