package com.devonfw.tools.ide.url.tool.rust;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link RustUrlUpdater} to allow integration testing with wiremock.
 */
public class RustUrlUpdaterMock extends RustUrlUpdater {

  private final String baseUrl;

  /**
   * The constructor.
   *
   * @param wireMockRuntimeInfo the {@link WireMockRuntimeInfo} holding the wiremock base URL.
   */
  public RustUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl + "/rustup.sh";
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }
}
