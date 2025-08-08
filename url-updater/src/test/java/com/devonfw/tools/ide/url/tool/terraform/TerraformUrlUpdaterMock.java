package com.devonfw.tools.ide.url.tool.terraform;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link TerraformUrlUpdater} to allow integration testing with wiremock.
 */
public class TerraformUrlUpdaterMock extends TerraformUrlUpdater {

  private final String baseUrl;

  public TerraformUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  public String getDownloadBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";
  }
}

