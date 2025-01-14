package com.devonfw.tools.ide.repo;

import java.util.Set;

import com.devonfw.tools.ide.os.OperatingSystem;
import com.devonfw.tools.ide.os.SystemArchitecture;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Representation of a {@link CustomToolMetadata} from a {@link CustomToolRepository}.
 */
public final class CustomToolMetadata implements UrlDownloadFileMetadata {

  private final String tool;

  private final VersionIdentifier version;

  private final OperatingSystem os;

  private final SystemArchitecture arch;

  private final String url;

  private final String checksum;

  private final String repositoryUrl;

  /**
   * The constructor.
   *
   * @param tool the {@link #getTool() tool}.
   * @param versionString the {@link #getVersion() version} as {@link String}.
   * @param os the {@link #getOs() OS}.
   * @param arch the {@link #getArch()  architecture}.
   * @param url the {@link #getUrl() download URL}.
   * @param checksum the {@link #getChecksum() checksum}.
   * @param repositoryUrl the {@link #getRepositoryUrl() repository URL}.
   */
  public CustomToolMetadata(String tool, String versionString, OperatingSystem os, SystemArchitecture arch,
      String url, String checksum, String repositoryUrl) {

    super();
    this.tool = tool;
    this.version = VersionIdentifier.of(versionString);
    this.os = os;
    this.arch = arch;
    this.url = url;
    this.checksum = checksum;
    this.repositoryUrl = repositoryUrl;
  }

  @Override
  public String getTool() {

    return this.tool;
  }

  @Override
  public String getEdition() {

    return this.tool;
  }

  @Override
  public VersionIdentifier getVersion() {

    return version;
  }

  /**
   * @return the URL to the download artifact.
   */
  public String getUrl() {

    return this.url;
  }

  @Override
  public String getChecksum() {

    return this.checksum;
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
  public Set<String> getUrls() {

    return Set.of(this.url);
  }

  /**
   * @return the {@link CustomToolsJson#url() repository base URL}.
   */
  @JsonIgnore
  public String getRepositoryUrl() {

    return this.repositoryUrl;
  }

  @Override
  public String toString() {

    return "CustomTool[" + this.tool + ":" + this.version + "@" + this.url + "]";
  }

}
