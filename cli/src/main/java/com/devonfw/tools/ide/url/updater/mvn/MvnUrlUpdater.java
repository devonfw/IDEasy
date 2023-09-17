package com.devonfw.tools.ide.url.updater.mvn;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for mvn (maven).
 */
public class MvnUrlUpdater extends WebsiteUrlUpdater {
  @Override
  protected String getTool() {

    return "mvn";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        "https://archive.apache.org/dist/maven/maven-3/${version}/binaries/apache-maven-${version}-bin.tar.gz");
  }

  @Override
  protected String getVersionUrl() {

    return "https://archive.apache.org/dist/maven/maven-3/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("(\\d\\.\\d\\.\\d)");
  }
}
