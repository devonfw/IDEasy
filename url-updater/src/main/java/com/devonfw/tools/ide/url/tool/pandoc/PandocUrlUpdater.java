package com.devonfw.tools.ide.url.tool.pandoc;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionComparisonResult;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for <a href="https://pandoc.org/">Pandoc</a>.
 */
public class PandocUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_PANDOC_VID = VersionIdentifier.of("3.1.2");

  /**
   * The Constructor.
   */
  public PandocUrlUpdater() {

    super();
  }

  /**
   * Package-private constructor used for testing {@link PandocUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  PandocUrlUpdater(String baseUrl) {

    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "pandoc";
  }

  @Override
  protected String getGithubOrganization() {

    return "jgm";
  }

  @Override
  protected String getGithubRepository() {

    return "pandoc";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    // GitHub release names are like "pandoc 3.12" while tags/download paths use "3.12".
    return "pandoc ";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    VersionComparisonResult versionComparisonResult = vid.compareVersion(MIN_PANDOC_VID);
    if (versionComparisonResult.isEqual() || versionComparisonResult.isGreater()) {
      String baseUrl = createGithubReleaseDownloadUrl("${version}", "pandoc-${version}-");
      doAddVersion(urlVersion, baseUrl + "linux-amd64.tar.gz", LINUX, X64);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);
      doAddVersion(urlVersion, baseUrl + "x86_64-macOS.zip", MAC, X64);
      doAddVersion(urlVersion, baseUrl + "arm64-macOS.zip", MAC, ARM64);
      doAddVersion(urlVersion, baseUrl + "windows-x86_64.zip", WINDOWS, X64);
    }
  }

  @Override
  public String getCpeVendor() {

    return "pandoc";
  }

  @Override
  public String getCpeProduct() {

    return "pandoc";
  }
}
