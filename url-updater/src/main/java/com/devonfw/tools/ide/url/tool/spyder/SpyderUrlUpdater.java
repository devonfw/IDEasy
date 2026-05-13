package com.devonfw.tools.ide.url.tool.spyder;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

public class SpyderUrlUpdater extends GithubUrlTagUpdater {

  private static final VersionIdentifier MIN_SPYDER_VID = VersionIdentifier.of("6.0.0");

  @Override
  public String getTool() {
    return "spyder";
  }

  @Override
  protected String getGithubOrganization() {
    return "spyder-ide";
  }

  @Override
  protected String getGithubRepository() {
    return "spyder";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    String baseUrl = createGithubReleaseDownloadUrl("v${version}", "Spyder-");
    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.compareVersion(MIN_SPYDER_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "Linux-x86_64.sh", LINUX, X64);

      doAddVersion(urlVersion, baseUrl + "macOS-x86_64.pkg", MAC, X64);
      doAddVersion(urlVersion, baseUrl + "macOS-arm64.pkg", MAC, ARM64);

      doAddVersion(urlVersion, baseUrl + "Windows-x86_64.exe", WINDOWS, X64);
    }
  }

  @Override
  protected String getVersionPrefixToRemove() {
    return "v";
  }

  @Override
  protected String[] getCustomVersionFilter() {
    return new String[] { "a", "b", "rc" };
  }


}
