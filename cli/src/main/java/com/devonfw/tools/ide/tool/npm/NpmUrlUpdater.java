package com.devonfw.tools.ide.tool.npm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.JsonUrlUpdater;
import com.devonfw.tools.ide.url.updater.WebsiteUrlUpdater;

import java.util.Collection;

/**
 * {@link WebsiteUrlUpdater} for npm (node package manager).
 */
public class NpmUrlUpdater extends JsonUrlUpdater<NpmJsonObject> {
  private static final String JSON_URL = "https://registry.npmjs.org/npm/";

  @Override
  protected String getTool() {

    return "npm";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = JSON_URL;
    doAddVersion(urlVersion, baseUrl + "-/npm-${version}.tgz");
  }

  @Override
  protected String doGetVersionUrl() {

    return JSON_URL;
  }

  @Override
  protected Class<NpmJsonObject> getJsonObjectType() {

    return NpmJsonObject.class;
  }

  @Override
  protected void collectVersionsFromJson(NpmJsonObject jsonItem, Collection<String> versions) {

    for (NpmJsonVersion item : jsonItem.getVersions().getVersions().values()) {
      String version = item.getVersion();
      addVersion(version, versions);
    }
  }

}