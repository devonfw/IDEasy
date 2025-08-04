package com.devonfw.tools.ide.url.tool.jenkins;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Jenkins.
 */
public class JenkinsUrlUpdater extends WebsiteUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.\\d{2,3}\\.\\d)");

  @Override
  protected String getTool() {

    return "jenkins";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/war-stable/";
  }

  @Override
  protected String getVersionBaseUrl() {

    return getDownloadBaseUrl();
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://mirrors.jenkins.io";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getDownloadBaseUrl() + "/war-stable/${version}/jenkins.war");
  }

  @Override
  public String getCpeVendor() {
    return "jenkinsci";
  }

  @Override
  public String getCpeProduct() {
    return "jenkins";
  }
}
