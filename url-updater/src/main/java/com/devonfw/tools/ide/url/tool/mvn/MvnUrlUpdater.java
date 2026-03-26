package com.devonfw.tools.ide.url.tool.mvn;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link MavenBasedUrlUpdater} for mvn (maven).
 */
public class MvnUrlUpdater extends MavenBasedUrlUpdater {

  public static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("3.0.4");

  @Override
  protected String getMavenGroupIdPath() {

    return "org/apache/maven";
  }

  @Override
  protected String getMavenArtifcatId() {

    return "maven-core";
  }

  @Override
  public String getTool() {

    return "mvn";
  }

  @Override
  protected boolean isValidVersion(String version) {

    VersionIdentifier artifactVersion = VersionIdentifier.of(version);
    if (artifactVersion != null) {
      return artifactVersion.isGreaterOrEqual(MIN_VERSION);
    }
    return true;
  }

  @Override
  public String getCpeVendor() {

    return "apache";
  }

  @Override
  public String getCpeProduct() {

    return "maven";
  }
}
