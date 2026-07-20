package com.devonfw.tools.ide.tool.uv;

import java.util.Collections;
import java.util.Set;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlChecksums;
import com.devonfw.tools.ide.url.model.file.UrlChecksumsEmpty;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link UrlDownloadFileMetadata} representing metadata of a uv tool artifact from PyPI.
 */
public class UvArtifactMetadata implements UrlDownloadFileMetadata {

  private final UvArtifact uvArtifact;
  private final String tool;
  private final String edition;
  private final VersionIdentifier version;

  /**
   * The constructor.
   *
   * @param uvArtifact the {@link UvArtifact}.
   * @param tool the tool name.
   * @param edition the edition.
   */
  public UvArtifactMetadata(UvArtifact uvArtifact, String tool, String edition) {
    this.uvArtifact = uvArtifact;
    this.version = VersionIdentifier.of(uvArtifact.getVersion());
    this.tool = tool;
    this.edition = edition;
  }

  /**
   * @return the {@link UvArtifact}
   */
  public UvArtifact getUvArtifact() {
    return this.uvArtifact;
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
    return this.uvArtifact.toString();
  }

}
