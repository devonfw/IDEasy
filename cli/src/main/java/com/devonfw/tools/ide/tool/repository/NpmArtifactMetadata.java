package com.devonfw.tools.ide.tool.repository;

import java.util.Collections;
import java.util.Set;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.tool.npm.NpmArtifact;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlChecksumsEmpty;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link UrlDownloadFileMetadata} representing Metadata of a {@link com.devonfw.tools.ide.tool.npm.Npm npm} artifact.
 */
public class NpmArtifactMetadata implements UrlDownloadFileMetadata {

  private final NpmArtifact npmArtifact;

  private final String tool;

  private final String edition;

  private final VersionIdentifier version;

  NpmArtifactMetadata(NpmArtifact npmArtifact, String tool, String edition) {

    this.npmArtifact = npmArtifact;
    this.version = VersionIdentifier.of(npmArtifact.getVersion());
    this.tool = tool;
    this.edition = edition;
  }

  /**
   * @return the {@link NpmArtifact}.
   */
  public NpmArtifact getNpmArtifact() {

    return this.npmArtifact;
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

    return Collections.emptySet();
  }

  @Override
  public OperatingSystem getOs() {

    return null;
  }

  @Override
  public SystemArchitecture getArch() {

    return null;
  }

  @Override
  public UrlChecksums getChecksums() {

    return UrlChecksumsEmpty.of();
  }

  @Override
  public String toString() {

    return this.npmArtifact.toString();
  }
}
