package com.devonfw.tools.ide.url.tool.docker;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;

/**
 * {@link GithubUrlTagUpdater} for the docker edition Rancher-Desktop.
 */
public class DockerRancherDesktopUrlUpdater extends GithubUrlTagUpdater {

  @Override
  public String getTool() {

    return "docker";
  }

  @Override
  protected String getEdition() {

    return "rancher";
  }

  @Override
  protected String getGithubOrganization() {

    return "rancher-sandbox";
  }

  @Override
  protected String getGithubRepository() {

    return "rancher-desktop";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/rancher-sandbox/rancher-desktop/releases/download/v${version}/";

    doAddVersion(urlVersion, baseUrl + "Rancher.Desktop.Setup.${version}.msi", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "Rancher.Desktop-${version}.x86_64.dmg", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "Rancher.Desktop-${version}.aarch64.dmg", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "rancher-desktop-linux-v${version}.zip", LINUX);

  }

  @Override
  public String getCpeVendor() {
    return "suse";
  }

  @Override
  public String getCpeProduct() {
    return "rancher_desktop";
  }
}
