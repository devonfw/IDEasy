package com.devonfw.tools.ide.url.updater.npm;

import java.util.regex.Pattern;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

/**
 * {@link WebsiteUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends WebsiteUrlUpdater {
  @Override
  protected String getTool() {

    return "npm";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {
    
    doAddVersion(urlVersion, "https://registry.npmjs.org/npm/-/npm-${version}.tgz");

  }

  @Override
  protected String getVersionUrl() {

    return "https://registry.npmjs.org/npm/";
  }

  @Override
  protected Pattern getVersionPattern() {

    return Pattern.compile("npm-(\\d\\.\\d{1,2}\\.\\d+)");
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "npm-";
  }

}