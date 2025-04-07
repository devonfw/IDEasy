package com.devonfw.tools.ide.url.tool.jasypt;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link MavenBasedUrlUpdater} for jasypt
 */
public class JasyptUrlUpdater extends MavenBasedUrlUpdater {

  public static final VersionIdentifier MIN_VERSION = VersionIdentifier.of("1.9.3");

  @Override
  protected String getTool() {

    return "jasypt";
  }

  @Override
  protected String getMavenGroupIdPath() {

    return "org/jasypt";
  }

  @Override
  protected String getMavenArtifcatId() {

    return "jasypt";
  }

  @Override
  public String getCpeVendor() {
    return "jasypt";
  }

  @Override
  public String getCpeProduct() {
    return "jasypt";
  }

  @Override
  public boolean isValidVersion(String version) {

    VersionIdentifier artifactVersion = VersionIdentifier.of(version);
    if (artifactVersion != null) {
      return artifactVersion.isGreaterOrEqual(MIN_VERSION);
    }
    return false;
  }

}
