package com.devonfw.tools.ide.url.updater.graalvm;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * {@link GraalVmUrlUpdater} for "oracle" edition of GraalVM.
 */
public class GraalVmOracleUrlUpdater extends GraalVmUrlUpdater {

  @Override
  protected String getEdition() {

    return "graalvm";
  }
  @Override
  protected String getVersionPrefixToRemove() {

    return "vm-";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${version}/graalvm-ce-java11-";
    doAddVersion(urlVersion, baseUrl + "windows-amd64-${version}.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "linux-amd64-${version}.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "darwin-amd64-${version}.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "darwin-aarch64-${version}.tar.gz", MAC, ARM64);
  }
}
