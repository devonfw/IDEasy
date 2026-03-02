package com.devonfw.tools.ide.url.tool.pgadmin;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link PgAdminUrlUpdater} to allow integration testing with wiremock.
 */
public class PgAdminUrlUpdaterMock extends PgAdminUrlUpdater {

  private final String baseUrl;

  public PgAdminUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
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

