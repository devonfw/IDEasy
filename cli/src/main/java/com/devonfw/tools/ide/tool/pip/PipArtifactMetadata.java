package com.devonfw.tools.ide.tool.pip;

import java.util.Collections;
import java.util.Set;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlChecksumsEmpty;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link UrlDownloadFileMetadata} representing Metadata of a {@link Pip pip} artifact from PyPI.
 */
public class PipArtifactMetadata implements UrlDownloadFileMetadata {

  private final PipArtifact pipArtifact;

  private final String tool;

  private final String edition;

  private final VersionIdentifier version;

  /**
   * The constructor.
   *
   * @param pipArtifact the {@link PipArtifact}.
   * @param tool the tool name.
   * @param edition the edition.
   */
  public PipArtifactMetadata(PipArtifact pipArtifact, String tool, String edition) {

    this.pipArtifact = pipArtifact;
    this.version = VersionIdentifier.of(pipArtifact.getVersion());
    this.tool = tool;
    this.edition = edition;
  }

  /**
   * @return the {@link PipArtifact}.
   */
  public PipArtifact getPipArtifact() {

    return this.pipArtifact;
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

    return this.pipArtifact.toString();
  }
}
