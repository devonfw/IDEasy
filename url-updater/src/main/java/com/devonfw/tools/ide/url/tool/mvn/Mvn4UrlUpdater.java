package com.devonfw.tools.ide.url.tool.mvn;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for mvn (maven) versions 4.x.
 */
public class Mvn4UrlUpdater extends WebsiteUrlUpdater {

  @Override
  protected String getTool() {

    return "mvn";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        "https://archive.apache.org/dist/maven/maven-4/${version}/binaries/apache-maven-${version}-bin.tar.gz");
  }

  @Override
  protected String mapVersion(String version) {

    return version;
  }

  @Override
  protected String getVersionUrl() {

    return "https://archive.apache.org/dist/maven/maven-4/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("(\\d\\.\\d\\.\\d-[a-z]*?-\\d{1,2})");
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
