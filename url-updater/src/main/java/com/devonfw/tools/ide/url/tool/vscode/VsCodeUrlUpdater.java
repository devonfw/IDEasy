package com.devonfw.tools.ide.url.tool.vscode;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for vscode (Visual Studio Code).
 */
public class VsCodeUrlUpdater extends GithubUrlUpdater {

  private static final String BASE_URL = "https://update.code.visualstudio.com";

  @Override
  protected String getTool() {

    return "vscode";
  }

  @Override
  protected String getGithubOrganization() {

    return "microsoft";
  }

  @Override
  protected String getBaseUrl() {

    return BASE_URL;
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getBaseUrl() + "/${version}/";
    doAddVersion(urlVersion, baseUrl + "win32-x64-archive/stable", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux-x64/stable", LINUX);
    doAddVersion(urlVersion, baseUrl + "darwin/stable", MAC);
  }

  @Override
  protected String mapVersion(String version) {

    if (version.matches("\\d+\\.\\d+\\.\\d+")) {
      return super.mapVersion(version);
    } else {
      return null;
    }
  }

  @Override
  public String getCpeVendor() {
    return "microsoft";
  }

  @Override
  public String getCpeProduct() {
    return "vscode";
  }

}
