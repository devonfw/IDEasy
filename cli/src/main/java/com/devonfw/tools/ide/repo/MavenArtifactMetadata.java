package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.util.Collections;
import java.util.Set;

/**
 * {@link UrlDownloadFileMetadata} representing Metadata of a maven artifact.
 */
public class MavenArtifactMetadata implements UrlDownloadFileMetadata {
  private final String groupId;

  private final String artifactId;

  private final VersionIdentifier version;

  private final Set<String> urls;

  private final OperatingSystem os;

  private final SystemArchitecture arch;

  MavenArtifactMetadata(String groupId, String artifactId, VersionIdentifier version, String downloadUrl,
      OperatingSystem os, SystemArchitecture arch) {

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.urls = Collections.singleton(downloadUrl);
    this.os = os;
    this.arch = arch;
  }

  @Override
  public String getTool() {

    return this.groupId;
  }

  @Override
  public String getEdition() {

    return this.artifactId;
  }

  @Override
  public VersionIdentifier getVersion() {

    return this.version;
  }

  @Override
  public Set<String> getUrls() {

    return this.urls;
  }

  @Override
  public OperatingSystem getOs() {

    return this.os;
  }

  @Override
  public SystemArchitecture getArch() {

    return this.arch;
  }

  @Override
  public String getChecksum() {

    return null;
  }
}