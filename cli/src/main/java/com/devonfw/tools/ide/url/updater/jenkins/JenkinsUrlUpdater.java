package com.devonfw.tools.ide.url.updater.jenkins;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Jenkins.
 */
public class JenkinsUrlUpdater extends WebsiteUrlUpdater {

  @Override
  protected String getTool() {

    return "jenkins";
  }

  @Override
  protected String getVersionUrl() {

    return "https://mirrors.jenkins.io/war-stable/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("(\\d\\.\\d{2,3}\\.\\d)");

  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, "https://mirrors.jenkins.io/war-stable/${version}/jenkins.war");
  }
}
