package com.devonfw.tools.ide.url.tool.graalvm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * {@link GraalVmUrlUpdater} for "community" edition of GraalVM.
 */
public class GraalVmCommunityUpdater extends GraalVmUrlUpdater {

  @Override
  protected String getEdition() {

    return "community";
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "jdk-";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/graalvm/graalvm-ce-builds/releases/download/jdk-${version}/graalvm-community-jdk-${version}_";
    doAddVersion(urlVersion, baseUrl + "windows-x64_bin.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "linux-x64_bin.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "macos-x64_bin.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "macos-aarch64_bin.tar.gz", MAC, ARM64);
  }

  @Override
  public String mapVersion(String version) {

    if (version.startsWith("jdk")) {
      return super.mapVersion(version);
    } else {
      return null;
    }
  }

}
