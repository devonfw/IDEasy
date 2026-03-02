package com.devonfw.tools.ide.url.tool.kotlinc;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for Kotlin.
 */
// TODO refactor me to GithubUrlUpdater
public class KotlincUrlUpdater extends WebsiteUrlUpdater {

  private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");

  @Override
  public String getTool() {

    return "kotlinc";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion,
        getDownloadBaseUrl() + "/JetBrains/kotlin/releases/download/v${version}/kotlin-compiler-${version}.zip");
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://github.com";
  }

  @Override
  protected String getVersionUrl() {

    return getVersionBaseUrl() + "/repos/JetBrains/kotlin/releases";
  }

  @Override
  protected String getVersionBaseUrl() {

    return "https://api.github.com";
  }

  @Override
  protected Pattern getVersionPattern() {

    return VERSION_PATTERN;
  }

  @Override
  public String getCpeVendor() {
    return "jetbrains";
  }

  @Override
  public String getCpeProduct() {
    return "kotlin";
  }
}
