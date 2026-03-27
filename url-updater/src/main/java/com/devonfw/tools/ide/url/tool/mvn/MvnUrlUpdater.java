package com.devonfw.tools.ide.url.tool.mvn;

import java.util.Locale;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.url.updater.GithubUrlReleaseUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link GithubUrlReleaseUpdater} for mvn (maven).
 */
public class MvnUrlUpdater extends GithubUrlReleaseUpdater {

  private static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("3.0.4");
  private static final VersionIdentifier MAVEN_4_IDENTIFIER = VersionIdentifier.of("4.0.0-alpha");

  @Override
  public String getTool() {

    return "mvn";
  }

  @Override
  public String getCpeVendor() {

    return "apache";
  }

  @Override
  public String getCpeProduct() {

    return "maven";
  }

  @Override
  protected String getGithubOrganization() {
    return "apache";
  }

  @Override
  protected String getGithubRepository() {

    return "maven";
  }

  @Override
  protected String getDownloadBaseUrl() {

    return "https://archive.apache.org";
  }

  @Override
  public String mapVersion(String version) {

    // Workaround to get Release Candidates.
    // A better solution would be a flag or a map to signal which version are wanted.
    String vLower = version.toLowerCase(Locale.ROOT);
    if (vLower.contains("-rc")) {
      return version;
    } else {
      return super.mapVersion(version);
    }
  }

  @Override
  protected void addVersion(UrlVersion urlVersion) {

    VersionIdentifier versionIdentifier = urlVersion.getVersionIdentifier();
    if (versionIdentifier.compareVersion(MIN_VERSION).isGreater()) {
      String version = mapVersion(urlVersion.getName());

      // This is just a workaround because apache archive sorts its versions into maven-x folders.
      // A better solution would be to extract the major version of the VersionIdentifier
      // and put that into the String Formatter but no such method exists yet.
      String majorFolder = versionIdentifier.compareVersion(MAVEN_4_IDENTIFIER).isLess() ? "maven-3" : "maven-4";
      doAddVersion(urlVersion, getDownloadBaseUrl() + "/dist/maven/" + majorFolder + "/${version}/binaries/apache-maven-${version}-bin.zip");
    }
  }
}
