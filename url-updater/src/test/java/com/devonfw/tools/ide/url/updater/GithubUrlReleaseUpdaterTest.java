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

/**
 * Tests for {@link GithubUrlReleaseUpdater} URL conventions.
 */
@WireMockTest
class GithubUrlReleaseUpdaterTest {

  @Test
  void testGithubReleaseUrlConventions() {

    TestGithubUpdater updater = new TestGithubUpdater();

    assertThat(updater.getVersionUrl()).isEqualTo("https://api.github.com/repos/acme/tool/releases");
    assertThat(updater.getReleaseDownloadBaseUrl()).isEqualTo("https://github.com/acme/tool/releases/download");
    assertThat(updater.getReleaseAssetUrl("v1.2.3", "asset.zip"))
        .isEqualTo("https://github.com/acme/tool/releases/download/v1.2.3/asset.zip");
  }

  @Test
  void testGithubReleasePagination(WireMockRuntimeInfo wmRuntimeInfo) {

    StringBuilder firstPage = new StringBuilder("[");
    for (int i = 0; i < 50; i++) {
      if (i > 0) {
        firstPage.append(",");
      }
      firstPage.append("{\"name\":\"1.0.").append(i).append("\",\"prerelease\":false}");
    }
    firstPage.append("]");

    String secondPage = """
        [
          {
            "name": "2.0.0",
            "prerelease": false
          }
        ]
        """;

    stubFor(get(urlEqualTo("/repos/acme/tool/releases?per_page=50&page=1"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(firstPage.toString())));

    stubFor(get(urlEqualTo("/repos/acme/tool/releases?per_page=50&page=2"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(secondPage)));

    TestGithubUpdater updater = new TestGithubUpdater(
        wmRuntimeInfo.getHttpBaseUrl(),
        wmRuntimeInfo.getHttpBaseUrl());

    String response = updater.getVersionResponse();

    assertThat(response).contains("\"name\":\"1.0.0\"");
    assertThat(response).contains("\"name\":\"1.0.49\"");
    assertThat(response).contains("\"name\":\"2.0.0\"");

    verify(getRequestedFor(
        urlEqualTo("/repos/acme/tool/releases?per_page=50&page=1")));

    verify(getRequestedFor(
        urlEqualTo("/repos/acme/tool/releases?per_page=50&page=2")));
  }

  private static final class TestGithubUpdater extends GithubUrlReleaseUpdater {

    TestGithubUpdater() {
      super();
    }

    TestGithubUpdater(String downloadBaseUrl, String versionBaseUrl) {
      super(downloadBaseUrl, versionBaseUrl);
    }

    String getVersionResponse() {
      return doGetVersionResponseBodyAsString(doGetVersionUrl());
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

    String getVersionUrl() {

      return doGetVersionUrl();
    }

    String getReleaseDownloadBaseUrl() {

      return getGithubReleaseDownloadBaseUrl();
    }

    String getReleaseAssetUrl(String releaseTag, String assetName) {

      return createGithubReleaseDownloadUrl(releaseTag, assetName);
    }
  }
}
