package com.devonfw.tools.ide.url.tool.soapui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for SoapUI.
 */
public class SoapUiUrlUpdater extends GithubUrlReleaseUpdater {

  private static final String DOWNLOAD_BASE_URL = "https://dl.eviware.com";

  private static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("5.8.0");

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");

  /**
   * The constructor.
   */
  public SoapUiUrlUpdater() {
    super(DOWNLOAD_BASE_URL);
  }

  /**
   * Package-private constructor used for testing {@link SoapUiUrlUpdater}.
   *
   * @param baseUrl mock url used as download and version base.
   */
  public SoapUiUrlUpdater(String baseUrl) {
    super(baseUrl, baseUrl);
  }

  @Override
  public String getTool() {
    return "soapui";
  }

  @Override
  protected String getGithubOrganization() {
    return "SmartBear";
  }

  @Override
  protected String getGithubRepository() {
    return "soapui";
  }

  @Override
  public String mapVersion(String version) {
    if (version == null) {
      return null;
    }
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (matcher.find()) {
      return super.mapVersion(matcher.group(1));
    }
    return null;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    if (urlVersion.getVersionIdentifier().compareVersion(MIN_VERSION).isLess()) {
      return;
    }
    String base = getDownloadBaseUrl() + "/soapuios/${version}/SoapUI-${version}-";
    doAddVersion(urlVersion, base + "windows-bin.zip", WINDOWS, X64);
    doAddVersion(urlVersion, base + "linux-bin.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, base + "mac-x64-bin.zip", MAC, X64);
    doAddVersion(urlVersion, base + "mac-arm64-bin.zip", MAC, ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "smartbear";
  }

  @Override
  public String getCpeProduct() {
    return "soapui";
  }
}
