package com.devonfw.tools.ide.url.tool.pip;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for pip (python installer of pacakges).
 */
public class PipUrlUpdater extends WebsiteUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d(\\.\\d)?)");

  @Override
  public String getTool() {

    return "pip";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getDownloadBaseUrl() + "/pip/${version}/get-pip.py");
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://bootstrap.pypa.io";
  }

  @Override
  protected boolean isValidContentType(String contentType) {
    // pip is not a binary download but a script with content-type `text/x-Python` so we override this check here
    return true;
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/pip/";
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
  public String getCpeVendor() {
    return "pypa";
  }

  @Override
  public String getCpeProduct() {
    return "pip";
  }
}
