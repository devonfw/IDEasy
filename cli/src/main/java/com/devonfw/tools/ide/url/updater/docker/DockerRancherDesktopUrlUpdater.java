package com.devonfw.tools.ide.url.updater.docker;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for the docker edition Rancher-Desktop.
 */
public class DockerRancherDesktopUrlUpdater extends GithubUrlUpdater {
  @Override
  protected String getTool() {

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

    String baseUrl = "https://github.com/rancher-sandbox/rancher-desktop/releases/download/v${version}/";

    doAddVersion(urlVersion, baseUrl + "Rancher.Desktop.Setup.${version}.exe", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "Rancher.Desktop-${version}.x86_64.dmg", MAC);
    doAddVersion(urlVersion, baseUrl + "Rancher.Desktop-${version}-mac.aarch64.zip", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "rancher-desktop-linux-v${version}.zip", LINUX);

  }
}
