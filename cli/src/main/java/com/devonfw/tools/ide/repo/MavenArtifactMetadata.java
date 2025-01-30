package com.devonfw.tools.ide.repo;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

import java.util.Collections;
import java.util.Set;

/**
 * {@link UrlDownloadFileMetadata} representing Metadata of a maven artifact.
 */
public class MavenArtifactMetadata implements UrlDownloadFileMetadata {

  private final MvnArtifact mvnArtifact;

  private final VersionIdentifier version;

  private final OperatingSystem os;

  private final SystemArchitecture arch;

  MavenArtifactMetadata(MvnArtifact mvnArtifact, VersionIdentifier version) {

    this(mvnArtifact, version, null, null);
  }

  MavenArtifactMetadata(MvnArtifact mvnArtifact, VersionIdentifier version, OperatingSystem os, SystemArchitecture arch) {

    this.mvnArtifact = mvnArtifact;
    this.version = version;
    this.os = os;
    this.arch = arch;
  }

  @Override
  public String getTool() {

    return this.mvnArtifact.getGroupId();
  }

  @Override
  public String getEdition() {

    return this.mvnArtifact.getArtifactId();
  }

  @Override
  public VersionIdentifier getVersion() {

    return this.version;
  }

  @Override
  public Set<String> getUrls() {

    return Collections.singleton(this.mvnArtifact.getDownloadUrl());
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
