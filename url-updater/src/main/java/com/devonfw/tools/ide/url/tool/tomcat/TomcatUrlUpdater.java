package com.devonfw.tools.ide.url.tool.tomcat;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for Tomcat.
 */
public class TomcatUrlUpdater extends GithubUrlTagUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://archive.apache.org";

  /**
   * The Constructor.
   */
  public TomcatUrlUpdater() {
    super(DOWNLOAD_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link TomcatUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  TomcatUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {

    return "tomcat";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        getDownloadBaseUrl() + "/dist/tomcat/tomcat-${major}/v${version}/bin/apache-tomcat-${version}.tar.gz");
  }

  @Override
  protected String getGithubOrganization() {

    return "apache";
  }

  @Override
  protected String getGithubRepository() {

    return "tomcat";
  }

  @Override
  public String getCpeVendor() {
    return "apache";
  }

  @Override
  public String getCpeProduct() {
    return "tomcat";
  }
}
