package com.devonfw.tools.ide.url.tool.uv;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.AbstractUrlUpdater;
import com.devonfw.tools.ide.url.updater.GithubUrlTagUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlTagUpdater} for uv.
 */
public class UvUrlUpdater extends GithubUrlTagUpdater {

  private static final VersionIdentifier MIN_VID = VersionIdentifier.of("0.5.25");

  @Override
  public String getTool() {

    return "uv";
  }

  @Override
  protected String getGithubOrganization() {

    return "astral-sh";
  }

  @Override
  protected String getGithubRepository() {

    return "uv";
  }

  @Override
  public String mapVersion(String version) {
    if (VersionIdentifier.of(version).isLess(MIN_VID)) {
      return null;
    }
    return super.mapVersion(version);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl() + "/" + getGithubOrganization() + "/" + getGithubRepository() + "/releases/download/${version}/uv-";

    doAddVersion(urlVersion, baseUrl + "x86_64-pc-windows-msvc.zip", WINDOWS, X64);
    doAddVersion(urlVersion, baseUrl + "x86_64-unknown-linux-gnu.tar.gz", LINUX, X64);
    doAddVersion(urlVersion, baseUrl + "aarch64-unknown-linux-gnu.tar.gz", LINUX, ARM64);
    doAddVersion(urlVersion, baseUrl + "x86_64-apple-darwin.tar.gz", MAC, X64);
    doAddVersion(urlVersion, baseUrl + "aarch64-apple-darwin.tar.gz", MAC, ARM64);
    doAddVersion(urlVersion, baseUrl + "aarch64-pc-windows-msvc.zip", WINDOWS, ARM64);
  }

  @Override
  public String getCpeVendor() {
    return "astral";
  }

  @Override
  public String getCpeProduct() {
    return "uv";
  }

  @Override
  protected void initCpe(AbstractUrlUpdater.CpeRegistry cpe) {

    cpe.addVendor("astral")
        .addVendorInfix("astral-sh")
        .addProduct("uv");
  }

}
