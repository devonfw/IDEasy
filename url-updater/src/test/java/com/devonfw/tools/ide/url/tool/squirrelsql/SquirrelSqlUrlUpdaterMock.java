package com.devonfw.tools.ide.url.tool.squirrelsql;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link SquirrelSqlUrlUpdater} to allow integration testing with wiremock.
 */
public class SquirrelSqlUrlUpdaterMock extends SquirrelSqlUrlUpdater {

  private final String baseUrl;

  SquirrelSqlUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {

    super();
    this.baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
  }

  @Override
  protected String getDownloadBaseUrl() {

    return this.baseUrl;
  }

  @Override
  protected String doGetVersionUrl() {

    return this.baseUrl + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/releases";
  }

}
