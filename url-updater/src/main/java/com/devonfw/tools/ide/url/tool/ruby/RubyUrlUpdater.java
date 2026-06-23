package com.devonfw.tools.ide.url.tool.ruby;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

public class RubyUrlUpdater extends GithubUrlReleaseUpdater {

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
      String baseUrl = createGithubReleaseDownloadUrl("RubyInstaller-${version}", "rubyinstaller-");

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
}
