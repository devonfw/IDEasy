package com.devonfw.tools.ide.url.updater.node;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for node.js.
 */
public class NodeUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_NODE_VID = VersionIdentifier.of("v3.9.9");

  private static final VersionIdentifier MIN_WIN_ARM_VID = VersionIdentifier.of("v19.9.9");

  private static final VersionIdentifier MIN_MAC_ARM_VID = VersionIdentifier.of("v15.9.9");

  @Override
  protected String getTool() {

    return "node";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected String getGithubOrganization() {

    return "nodejs";
  }

  @Override
  protected String getGithubRepository() {

    return "node";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();

    if (vid.compareVersion(MIN_NODE_VID).isGreater()) {
      String baseUrl = "https://nodejs.org/dist/${version}/node-${version}-";
      doAddVersion(urlVersion, baseUrl + "win-x64.zip", WINDOWS);
      if (vid.compareVersion(MIN_WIN_ARM_VID).isGreater()) {
        doAddVersion(urlVersion, baseUrl + "win-arm64.zip", WINDOWS, ARM64);
      }
      doAddVersion(urlVersion, baseUrl + "linux-x64.tar.gz", LINUX);
      doAddVersion(urlVersion, baseUrl + "linux-arm64.tar.gz", LINUX, ARM64);
      doAddVersion(urlVersion, baseUrl + "darwin-x64.tar.gz", MAC);
      if (vid.compareVersion(MIN_MAC_ARM_VID).isGreater()) {
        doAddVersion(urlVersion, baseUrl + "darwin-arm64.tar.gz", MAC, ARM64);
      }
    }
  }

  @Override
  protected String mapVersion(String version) {

    return super.mapVersion("v" + version);
  }

}
