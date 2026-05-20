package com.devonfw.tools.ide.url.tool.vscode;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for vscode (Visual Studio Code).
 */
public class VsCodeUrlUpdater extends GithubUrlTagUpdater {

  @Override
  public String getTool() {

    return "vscode";
  }

  @Override
  protected String getGithubOrganization() {

    return "microsoft";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://update.code.visualstudio.com";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/${version}/";
    doAddVersion(urlVersion, baseUrl + "win32-x64-archive/stable", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux-x64/stable", LINUX);
    doAddVersion(urlVersion, baseUrl + "darwin/stable", MAC);
    doAddVersion(urlVersion, baseUrl + "darwin-arm64/stable", MAC, ARM64);
  }

  @Override
  public String mapVersion(String version) {

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
