package com.devonfw.tools.ide.url.updater;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Tests for {@link GithubUrlReleaseUpdater} URL conventions.
 */
class GithubUrlReleaseUpdaterTest {

  @Test
  void testGithubReleaseUrlConventions() {

    TestGithubUpdater updater = new TestGithubUpdater();

    assertThat(updater.getVersionUrl()).isEqualTo("https://api.github.com/repos/acme/tool/releases");
    assertThat(updater.getReleaseDownloadBaseUrl()).isEqualTo("https://github.com/acme/tool/releases/download");
    assertThat(updater.getReleaseAssetUrl("v1.2.3", "asset.zip"))
        .isEqualTo("https://github.com/acme/tool/releases/download/v1.2.3/asset.zip");
  }

  private static final class TestGithubUpdater extends GithubUrlReleaseUpdater {

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

