package com.devonfw.tools.ide.tool.repository;

import java.util.Collections;
import java.util.Set;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link UrlDownloadFileMetadata} representing Metadata of a maven artifact.
 */
public class MavenArtifactMetadata implements UrlDownloadFileMetadata {

  private final MvnArtifact mvnArtifact;

  private final String tool;

  private final String edition;

  private final VersionIdentifier version;

  private final UrlChecksums checksums;

  private final OperatingSystem os;

  private final SystemArchitecture arch;

  MavenArtifactMetadata(MvnArtifact mvnArtifact, String tool, String edition, UrlChecksums checksums) {

    this(mvnArtifact, tool, edition, checksums, null, null);
  }

  MavenArtifactMetadata(MvnArtifact mvnArtifact, String tool, String edition, UrlChecksums checksums, OperatingSystem os, SystemArchitecture arch) {

    this.mvnArtifact = mvnArtifact;
    this.version = VersionIdentifier.of(mvnArtifact.getVersion());
    this.tool = tool;
    this.edition = edition;
    this.checksums = checksums;
    this.os = os;
    this.arch = arch;
  }

  /**
   * @return the {@link MvnArtifact}.
   */
  public MvnArtifact getMvnArtifact() {

    return this.mvnArtifact;
  }

  @Override
  public String getTool() {

    return this.tool;
  }

  @Override
  public String getEdition() {

    return this.edition;
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
  public UrlChecksums getChecksums() {

    return this.checksums;
  }

  @Override
  public String toString() {

    return this.mvnArtifact.toString();
  }
}
