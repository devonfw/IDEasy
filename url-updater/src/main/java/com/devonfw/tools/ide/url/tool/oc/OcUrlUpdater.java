package com.devonfw.tools.ide.url.tool.oc;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for oc (openshift CLI).
 */
public class OcUrlUpdater extends WebsiteUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d\\.\\d\\.\\d*)");

  @Override
  public String getTool() {

    return "oc";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://mirror.openshift.com";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/pub/openshift-v4/clients/ocp/${version}/openshift-client-";
    doAddVersion(urlVersion, baseUrl + "windows-${version}.zip", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux-${version}.tar.gz", LINUX);
    doAddVersion(urlVersion, baseUrl + "mac-${version}.tar.gz", MAC);
  }

  @Override
  public String getCpeVendor() {
    return "openshift";
  }

  @Override
  public String getCpeProduct() {
    return "oc";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/pub/openshift-v4/clients/ocp/";
  }

  @Override
  protected String getVersionBaseUrl() {

    return getDownloadBaseUrl();
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }
}
