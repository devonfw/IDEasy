package com.devonfw.tools.ide.url.tool.ruby;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for RubyInstaller releases on GitHub.
 */
public class RubyUrlUpdater extends GithubUrlReleaseUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://github.com";

  private static final String VERSION_BASE_URL = "https://api.github.com";

  /**
   * The Constructor.
   */
  public RubyUrlUpdater() {
    super(DOWNLOAD_BASE_URL, VERSION_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link RubyUrlUpdater}.
   *
   * @param downloadBaseUrl mock url used as download base.
   * @param versionBaseUrl mock url used as version base.
   */
  RubyUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  // Older RubyInstaller releases are ignored because releases before 2.5.3-1 use unsupported legacy tag naming.
  private static final VersionIdentifier MIN_RUBY_VID = VersionIdentifier.of("2.5.3-1");

  @Override
  public String getTool() {
    return "ruby";
  }

  @Override
  protected String getGithubOrganization() {
    return "oneclick";
  }

  @Override
  protected String getGithubRepository() {
    return "rubyinstaller2";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    VersionComparisonResult result = vid.compareVersion(MIN_RUBY_VID);

    if (result.isEqual() || result.isGreater()) {
      String baseUrl = createGithubReleaseDownloadUrl("RubyInstaller-${version}", "rubyinstaller-${version}-");

      doAddVersion(urlVersion, baseUrl + "x64.7z", WINDOWS, X64);
    }
  }


  @Override
  protected String getVersionPrefixToRemove() {
    return "RubyInstaller-";
  }


  @Override
  public String getCpeVendor() {
    return "ruby-lang";
  }

  @Override
  public String getCpeProduct() {
    return "ruby";
  }


  @Override
  public String mapVersion(String version) {

    int dateSeparator = version.indexOf(" - ");
    if (dateSeparator >= 0) {
      version = version.substring(0, dateSeparator);
    }
    return super.mapVersion(version);
  }

}
