package com.devonfw.tools.ide.url.tool.aws;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlUpdater;

/**
 * {@link GithubUrlUpdater} for AWS-CLI.
 */
public class AwsUrlUpdater extends GithubUrlUpdater {

  @Override
  public String getTool() {

    return "aws";
  }

  @Override
  protected String getGithubOrganization() {

    return "aws";
  }

  @Override
  protected String getGithubRepository() {

    return "aws-cli";
  }

  @Override
  protected String getDownloadBaseUrl() {
    return "https://awscli.amazonaws.com";
  }

  @Override
  public String mapVersion(String version) {

    int majorEnd = 0;
    int len = version.length();
    while (majorEnd < len) {
      char c = version.charAt(majorEnd);
      if ((c >= '0') && (c <= '9')) {
        majorEnd++;
      } else {
        break;
      }
    }
    if (majorEnd > 0) {
      int major = Integer.parseInt(version.substring(0, majorEnd));
      if (major < 2) {
        return null; // There are no valid download links for aws-cli below version 2
      }
    }
    return super.mapVersion(version);
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    String baseUrl = getDownloadBaseUrl();
    boolean ok = doAddVersion(urlVersion, baseUrl + "/AWSCLIV2-${version}.msi", OperatingSystem.WINDOWS);
    if (!ok) {
      return;
    }
    doAddVersion(urlVersion, baseUrl + "/awscli-exe-linux-x86_64-${version}.zip", OperatingSystem.LINUX);
    doAddVersion(urlVersion, baseUrl + "/AWSCLIV2-${version}.pkg", OperatingSystem.MAC);
  }

  @Override
  public String getCpeVendor() {
    return "amazon";
  }

  @Override
  public String getCpeProduct() {
    return "aws";
  }

}
