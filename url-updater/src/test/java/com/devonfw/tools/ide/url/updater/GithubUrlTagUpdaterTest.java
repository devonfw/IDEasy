package com.devonfw.tools.ide.url.updater;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class GithubUrlTagUpdaterTest {

  @Test
  void testGithubTagPagination(WireMockRuntimeInfo wmRuntimeInfo) {

    StringBuilder firstPage = new StringBuilder("[");
    for (int i = 0; i < 50; i++) {
      if (i > 0) {
        firstPage.append(",");
      }
      firstPage.append("{\"name\":\"1.0.").append(i).append("\"}");
    }
    firstPage.append("]");

    String secondPage = """
        [
          {
            "name": "2.0.0"
          }
        ]
        """;

    stubFor(get(urlEqualTo("/repos/acme/tool/tags?per_page=50&page=1"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(firstPage.toString())));

    stubFor(get(urlEqualTo("/repos/acme/tool/tags?per_page=50&page=2"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(secondPage)));

    TestGithubUpdater updater = new TestGithubUpdater(
        wmRuntimeInfo.getHttpBaseUrl(),
        wmRuntimeInfo.getHttpBaseUrl());

    String response = updater.getVersionResponse(Set.of());

    assertThat(response).contains("\"name\":\"1.0.0\"");
    assertThat(response).contains("\"name\":\"1.0.49\"");
    assertThat(response).contains("\"name\":\"2.0.0\"");

    verify(getRequestedFor(
        urlEqualTo("/repos/acme/tool/tags?per_page=50&page=1")));

    verify(getRequestedFor(
        urlEqualTo("/repos/acme/tool/tags?per_page=50&page=2")));
  }

  @Test
  void testGithubTagPaginationStopsAtKnownVersion(WireMockRuntimeInfo wmRuntimeInfo) {

    StringBuilder firstPage = new StringBuilder("[");
    for (int i = 0; i < 50; i++) {
      if (i > 0) {
        firstPage.append(",");
      }
      firstPage.append("{\"name\":\"3.0.").append(i).append("\"}");
    }
    firstPage.append("]");

    StringBuilder secondPage = new StringBuilder("[");
    for (int i = 0; i < 50; i++) {
      if (i > 0) {
        secondPage.append(",");
      }
      secondPage.append("{\"name\":\"2.0.").append(49 - i).append("\"}");
    }
    secondPage.append("]");

    stubFor(get(urlEqualTo("/repos/acme/tool/tags?per_page=50&page=1"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(firstPage.toString())));

    stubFor(get(urlEqualTo("/repos/acme/tool/tags?per_page=50&page=2"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(secondPage.toString())));

    TestGithubUpdater updater = new TestGithubUpdater(
        wmRuntimeInfo.getHttpBaseUrl(),
        wmRuntimeInfo.getHttpBaseUrl());

    String response = updater.getVersionResponse(Set.of("2.0.0"));

    assertThat(response).contains("\"name\":\"3.0.0\"");
    assertThat(response).contains("\"name\":\"2.0.1\"");
    assertThat(response).contains("\"name\":\"2.0.0\"");

    verify(getRequestedFor(
        urlEqualTo("/repos/acme/tool/tags?per_page=50&page=1")));

    verify(getRequestedFor(
        urlEqualTo("/repos/acme/tool/tags?per_page=50&page=2")));

    verify(0, getRequestedFor(
        urlEqualTo("/repos/acme/tool/tags?per_page=50&page=3")));
  }

  private static final class TestGithubUpdater extends GithubUrlTagUpdater {

    TestGithubUpdater(String downloadBaseUrl, String versionBaseUrl) {
      super(downloadBaseUrl, versionBaseUrl);
    }

    String getVersionResponse(Set<String> existingVersions) {
      return doGetVersionResponseBodyAsString(doGetVersionUrl(), existingVersions);
    }

    @Override
    public String getTool() {

      return "tool";
    }

    @Override
    protected String getGithubOrganization() {

      return "acme";
    }

    @Override
    protected Set<String> getVersions() {

      return Set.of();
    }

    @Override
    protected void addVersion(UrlVersion urlVersion) {

      // not needed for this test
    }
  }
}
