package com.devonfw.tools.ide.url.tool.gcloganalyzer;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Gc Log Analyzer.
 */
public class GcLogAnalyzerUrlUpdater extends WebsiteUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("Version\\s+(\\d+\\.\\d+\\.\\d+)");

  @Override
  public String getTool() {

    return "gcloganalyzer";
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://www.azul.com";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/products/components/gc-log-analyzer/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://cdn.azul.com";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String version = urlVersion.getName();
    String downloadUrl = getDownloadBaseUrl() + "/gcla/" + version + "/GCLogAnalyzer-" + version + "-ca.zip";

    doAddVersion(urlVersion, downloadUrl);
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

