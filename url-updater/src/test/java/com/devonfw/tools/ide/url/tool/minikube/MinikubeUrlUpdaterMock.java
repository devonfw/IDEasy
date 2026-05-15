package com.devonfw.tools.ide.url.tool.minikube;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Mock of {@link MinikubeUrlUpdater} to allow integration testing with wiremock.
 */
public class MinikubeUrlUpdaterMock extends MinikubeUrlUpdater {

  private final String baseUrl;

  private final WireMockRuntimeInfo wmRuntimeInfo;

  MinikubeUrlUpdaterMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
    super();
    this.wmRuntimeInfo = wireMockRuntimeInfo;
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
