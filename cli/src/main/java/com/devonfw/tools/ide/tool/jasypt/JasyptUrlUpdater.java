package com.devonfw.tools.ide.tool.jasypt;

import com.devonfw.tools.ide.url.updater.MavenBasedUrlUpdater;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.regex.Pattern;

/**
 * {@link MavenBasedUrlUpdater} for jasypt
 */
public class JasyptUrlUpdater extends MavenBasedUrlUpdater {

  public static final DefaultArtifactVersion MIN_VERSION = new DefaultArtifactVersion("1.9.3");

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
  public boolean isValidVersion(String version) {

    DefaultArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
    return artifactVersion.compareTo(MIN_VERSION) >= 0;
  }

}
