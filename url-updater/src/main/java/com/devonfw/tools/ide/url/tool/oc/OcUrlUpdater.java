package com.devonfw.tools.ide.url.tool.oc;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for oc (openshift CLI).
 */
public class OcUrlUpdater extends WebsiteUrlUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://mirror.openshift.com";
  private static final String VERSION_BASE_URL = "https://mirror.openshift.com";

  // Match only exact version directories (e.g. 4.22.19), skipping -rc folders and partial version prefixes.
  private static final Pattern VERSION_PATTERN = Pattern.compile("(?<=href=\")(\\d+\\.\\d+\\.\\d+)(?![\\w.-])");

  /**
   * The Constructor.
   */
  public OcUrlUpdater() {
    super(DOWNLOAD_BASE_URL, VERSION_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link OcUrlUpdater}.
   *
   * @param downloadBaseUrl mock URL used as download base.
   * @param versionBaseUrl mock URL used as version base.
   */
  OcUrlUpdater(String downloadBaseUrl, String versionBaseUrl) {
    super(downloadBaseUrl, versionBaseUrl);
  }

  @Override
  public String getTool() {

    return "oc";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/pub/openshift-v4/clients/ocp/${version}/openshift-client-";
    doAddVersion(urlVersion, baseUrl + "windows-${version}.zip", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux-${version}.tar.gz", LINUX);
    doAddVersion(urlVersion, baseUrl + "mac-${version}.tar.gz", MAC);
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/pub/openshift-v4/clients/ocp/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }
}
