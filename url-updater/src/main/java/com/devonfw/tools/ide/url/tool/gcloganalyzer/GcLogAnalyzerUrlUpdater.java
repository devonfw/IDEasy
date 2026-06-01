package com.devonfw.tools.ide.url.tool.gcloganalyzer;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Gc Log Analyzer.
 */
public class GcLogAnalyzerUrlUpdater extends WebsiteUrlUpdater {


  private static final Pattern VERSION_PATTERN =
      Pattern.compile("\\b\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?\\b");


  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  public String getTool() {

    return "gcloganalyzer";
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://docs.azul.com";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/gc-log-analyzer/release-notes";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://cdn.azul.com";
  }


  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName();
    String downloadVersion = getDownloadVersion(version);
    String downloadUrl = getDownloadBaseUrl() + "/gcla/" + downloadVersion + "/GCLogAnalyzer-" + downloadVersion + "-ca.zip";

    doAddVersion(urlVersion, downloadUrl);
  }

  private String getDownloadVersion(String version) {

    if (version.endsWith(".0.0")) {
      return version.substring(0, version.length() - 2);
    }

    return version;
  }

  @Override
  public String getCpeVendor() {

    return "azul";
  }

  @Override
  public String getCpeProduct() {

    return "gcloganalyzer";
  }

}

