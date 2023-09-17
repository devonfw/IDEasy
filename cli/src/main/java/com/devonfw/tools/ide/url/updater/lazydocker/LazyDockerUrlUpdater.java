package com.devonfw.tools.ide.url.updater.lazydocker;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for lazydocker.
 */
public class LazyDockerUrlUpdater extends GithubUrlUpdater {
  
  private static final VersionIdentifier MIN_WIN_VID = VersionIdentifier.of("0.7.4");
  
  private static final VersionIdentifier MIN_ARM_VID = VersionIdentifier.of("0.15.0");

  @Override
  protected String getTool() {

    return "lazydocker";
  }

  @Override
  protected String getGithubOrganization() {

    return "jesseduffield";
  }

  @Override
  protected String getGithubRepository() {

    return "lazydocker";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    String baseUrl = "https://github.com/jesseduffield/lazydocker/releases/download/v${version}/lazydocker_${version}_";
    if (vid.compareVersion(MIN_WIN_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "Windows_x86_64.zip", WINDOWS, X64);
    }
    doAddVersion(urlVersion, baseUrl + "Linux_x86_64.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "Linux_arm64.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "Darwin_x86_64.tar.gz", MAC, X64);
    if (vid.compareVersion(MIN_ARM_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "Windows_arm64.zip", WINDOWS, ARM64);
      doAddVersion(urlVersion, baseUrl + "Darwin_arm64.tar.gz", MAC, ARM64);
    }
  }

}
