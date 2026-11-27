package com.devonfw.tools.ide.url.updater;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.github.GithubRelease;
import com.devonfw.tools.ide.github.GithubReleases;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Tests for {@link GithubUrlReleaseUpdater} URL conventions and its generic pre-release filtering.
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

  /**
   * By default a GitHub release flagged {@code prerelease} is dropped even if its version string looks stable (which the suffix-based
   * {@code isAcceptVersion} check cannot catch, e.g. Ollama {@code v0.40.0-rc3} tagged {@code v0.40.0}), while a stable release is kept.
   */
  @Test
  void testGetVersionItemsFiltersOutPreReleasesByDefault() {

    GithubReleases releases = new GithubReleases();
    releases.add(new GithubRelease("1.0.0", false));
    releases.add(new GithubRelease("2.0.0-rc-1", true));

    Collection<GithubRelease> items = new GithubTestUpdater().getVersionItems(releases);

    assertThat(items).extracting(GithubRelease::name).containsExactly("1.0.0");
  }

  /**
   * When {@link GithubUrlReleaseUpdater#acceptPreReleases()} is {@code true}, pre-releases are kept alongside stable releases.
   */
  @Test
  void testGetVersionItemsKeepsPreReleasesWhenOptedIn() {

    GithubReleases releases = new GithubReleases();
    releases.add(new GithubRelease("1.0.0", false));
    releases.add(new GithubRelease("2.0.0-rc-1", true));

    Collection<GithubRelease> items = new AcceptPreReleasesUpdater().getVersionItems(releases);

    assertThat(items).extracting(GithubRelease::name).containsExactly("1.0.0", "2.0.0-rc-1");
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

  /**
   * Minimal concrete {@link GithubUrlReleaseUpdater} that keeps the default pre-release filtering.
   */
  private static class GithubTestUpdater extends GithubUrlReleaseUpdater {

    @Override
    public String getTool() {

      return "tool";
    }

    @Override
    protected String getGithubOrganization() {

      return "org";
    }

    @Override
    protected Set<String> getVersions() {

      return Set.of();
    }
  }

  /**
   * {@link GithubTestUpdater} that opts in to keep GitHub pre-release releases.
   */
  private static final class AcceptPreReleasesUpdater extends GithubTestUpdater {

    @Override
    protected boolean acceptPreReleases() {

      return true;
    }
  }
}
