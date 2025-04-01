package com.devonfw.tools.ide.url.tool.terraform;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for terraform.
 */
public class TerraformUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_MAC_ARM_VID = VersionIdentifier.of("1.1.0");
  private static final String BASE_URL = "https://releases.hashicorp.com";

  @Override
  protected String getTool() {

    return "terraform";
  }

  @Override
  protected String getGithubOrganization() {

    return "hashicorp";
  }

  @Override
  protected String getBaseUrl() {

    return BASE_URL;
  }

  @Override
  protected String getVersionPrefixToRemove() {

    return "v";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier vid = urlVersion.getVersionIdentifier();
    String baseUrl = getBaseUrl() + "/terraform/${version}/terraform_${version}_";
    doAddVersion(urlVersion, baseUrl + "windows_amd64.zip", WINDOWS);
    doAddVersion(urlVersion, baseUrl + "linux_amd64.zip", LINUX);
    doAddVersion(urlVersion, baseUrl + "darwin_amd64.zip", MAC);
    if (vid.compareVersion(MIN_MAC_ARM_VID).isGreater()) {
      doAddVersion(urlVersion, baseUrl + "darwin_arm64.zip", MAC, ARM64);
    }
  }

  @Override
  public String getCpeVendor() {

    return "hashicorp";
  }

  @Override
  public String getCpeProduct() {

    return "terraform";
  }

}
