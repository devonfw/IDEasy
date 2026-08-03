package com.devonfw.tools.ide.url.tool.jenkins;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Jenkins.
 */
public class JenkinsUrlUpdater extends WebsiteUrlUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://mirrors.jenkins.io";
  private static final String VERSION_BASE_URL = "https://mirrors.jenkins.io";

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.\\d{2,3}\\.\\d)");

  /**
   * The Constructor.
   */
  public JenkinsUrlUpdater() {
    super(DOWNLOAD_BASE_URL, VERSION_BASE_URL);
  }

  @Override
  public String getTool() {

    return "jenkins";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/war-stable/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getDownloadBaseUrl() + "/war-stable/${version}/jenkins.war");
  }

  @Override
  protected void initCpe(CpeRegistry cpe) {
    cpe.addVendor("jenkins").addVendor("jenkinsci").addProduct("jenkins");
  }
}
