package com.devonfw.tools.ide.url.tool.mn;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for Micronaut CLI ({@code mn}).
 */
public class MicronautUrlUpdater extends GithubUrlTagUpdater {

  private static final VersionIdentifier MIN_MICRONAUT_VID = VersionIdentifier.ofRequired("3.0.0");

  /**
   * The Constructor.
   */
  public MicronautUrlUpdater() {

    super();
  }

  /**
   * Package-private constructor used for testing {@link MicronautUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  MicronautUrlUpdater(String baseUrl) {

    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "mn";
  }

  @Override
  protected String getGithubOrganization() {

    return "micronaut-projects";
  }

  @Override
  protected String getGithubRepository() {

    return "micronaut-starter";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    if (vid.isStable() && vid.isGreaterOrEqual(MIN_MICRONAUT_VID)) {
      String downloadUrl = createGithubReleaseDownloadUrl("v${version}", "micronaut-cli-${version}.zip");
      doAddVersion(urlVersion, downloadUrl);
    }
  }

  @Override
  public String getCpeVendor() {

    return "objectcomputing";
  }

  @Override
  public String getCpeProduct() {

    return "micronaut";
  }
}
