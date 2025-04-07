package com.devonfw.tools.ide.url.tool.aws;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link AwsUrlUpdater} to allow integration testing with wiremock.
 */
public class AwsGithubUrlUpdaterMock extends AwsUrlUpdater {

  private final WireMockRuntimeInfo wmRuntimeInfo;

  /**
   * The constructor
   *
   * @param wmRuntimeInfo the {@link WireMockRuntimeInfo} holding the http url and port of the wiremock server.
   */
  public AwsGithubUrlUpdaterMock(WireMockRuntimeInfo wmRuntimeInfo) {
    this.wmRuntimeInfo = wmRuntimeInfo;
  }

  @Override
  protected String getBaseUrl() {
    return this.wmRuntimeInfo.getHttpBaseUrl() + "/download/";
  }

  @Override
  protected String doGetVersionUrl() {

    return this.wmRuntimeInfo.getHttpBaseUrl() + "/repos/" + getGithubOrganization() + "/" + getGithubRepository() + "/git/refs/tags";

  }

}
