package com.devonfw.tools.ide.url.tool.az;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlUpdater} for Azure-CLI.
 */
public class AzureUrlUpdater extends GithubUrlUpdater {

  private static final VersionIdentifier MIN_AZURE_VID = VersionIdentifier.of("2.17.0");
  private static final String BASE_URL = "https://azcliprod.blob.core.windows.net";

  @Override
  protected String getTool() {

    return "az";
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    doAddVersion(urlVersion, getBaseUrl() + "/msi/azure-cli-${version}.msi",
        OperatingSystem.WINDOWS);
  }

  @Override
  protected String getGithubOrganization() {

    return "Azure";
  }

  @Override
  protected String getGithubRepository() {

    return "azure-cli";
  }

  @Override
  protected String getBaseUrl() {

    return BASE_URL;
  }

  @Override
  protected String mapVersion(String version) {

    version = version.substring(version.lastIndexOf("-") + 1);
    VersionIdentifier vid = VersionIdentifier.of(version);
    if (vid.isValid() && vid.compareVersion(MIN_AZURE_VID).isGreater()) {
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
    return "az";
  }
}
