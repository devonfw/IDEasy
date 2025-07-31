package com.devonfw.tools.ide.url.tool.mvn;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * Abstract base class of URL updater for mvn (maven).
 */
public abstract class AbstractMvnUrlUpdater extends WebsiteUrlUpdater {

  @Override
  protected String getTool() {

    return "mvn";
  }

  protected abstract String getMvnVersionFolder();

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        getDownloadBaseUrl() + "/dist/maven/" + getMvnVersionFolder() + "/${version}/binaries/apache-maven-${version}-bin.tar.gz");
  }

  @Override
  protected String mapVersion(String version) {

    return version;
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/dist/maven/" + getMvnVersionFolder() + "/";
  }

  @Override
  protected String getVersionBaseUrl() {

    return getDownloadBaseUrl();
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://archive.apache.org";
  }

  @Override
  public String getCpeVendor() {

    return "apache";
  }

  @Override
  public String getCpeProduct() {

    return "maven";
  }
}
