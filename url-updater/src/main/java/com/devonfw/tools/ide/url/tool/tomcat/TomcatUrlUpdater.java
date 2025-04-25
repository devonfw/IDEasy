package com.devonfw.tools.ide.url.tool.tomcat;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for Tomcat.
 */
public class TomcatUrlUpdater extends GithubUrlUpdater {

  private static final String BASE_URL = "https://archive.apache.org";

  @Override
  protected String getTool() {

    return "tomcat";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        getBaseUrl() + "/dist/tomcat/tomcat-${major}/v${version}/bin/apache-tomcat-${version}.tar.gz");
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
  protected String getBaseUrl() {

    return BASE_URL;
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
