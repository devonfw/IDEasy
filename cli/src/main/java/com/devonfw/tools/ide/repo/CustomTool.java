package com.devonfw.tools.ide.repo;

import java.util.Set;

import com.devonfw.tools.ide.common.OperatingSystem;
import com.devonfw.tools.ide.common.SystemArchitecture;
import com.devonfw.tools.ide.common.SystemInfo;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Representation of a {@link CustomTool} from a {@link CustomToolRepository}.
 */
public final class CustomTool implements UrlDownloadFileMetadata {

  private final String tool;

  private final VersionIdentifier version;

  private final boolean osAgnostic;

  private final boolean archAgnostic;

  private final String repositoryUrl;

  private final String url;

  private final String checksum;

  private final OperatingSystem os;

  private final SystemArchitecture arch;

  /**
   * The constructor.
   *
   * @param tool the {@link #getTool() tool}.
   * @param versionIdentifier the {@link #getVersion() version}.
   * @param osAgnostic the {@link #isOsAgnostic() OS-agnostic flag}.
   * @param archAgnostic the {@link #isArchAgnostic() architecture-agnostic flag}.
   * @param repositoryUrl the {@link #getRepositoryUrl() repository URL}.
   * @param checksum the {@link #getChecksum() checksum}.
   * @param systemInfo the {@link SystemInfo}.
   */
  public CustomTool(String tool, VersionIdentifier versionIdentifier, boolean osAgnostic, boolean archAgnostic,
      String repositoryUrl, String checksum, SystemInfo systemInfo) {

    super();
    this.tool = tool;
    this.version = versionIdentifier;
    this.osAgnostic = osAgnostic;
    this.archAgnostic = archAgnostic;
    this.repositoryUrl = repositoryUrl;
    String versionString = versionIdentifier.toString();
    int capacity = repositoryUrl.length() + 2 * tool.length() + 2 * versionString.length() + 7;
    if (osAgnostic) {
      this.os = null;
    } else {
      this.os = systemInfo.getOs();
      capacity += this.os.toString().length() + 1;
    }
    if (archAgnostic) {
      this.arch = null;
    } else {
      this.arch = systemInfo.getArchitecture();
      capacity += this.arch.toString().length() + 1;
    }
    this.checksum = checksum;
    StringBuilder sb = new StringBuilder(capacity);
    sb.append(this.repositoryUrl);
    char last = this.repositoryUrl.charAt(repositoryUrl.length() - 1);
    if ((last != '/') && (last != '\\')) {
      sb.append('/');
    }
    sb.append(tool);
    sb.append('/');
    sb.append(versionString);
    sb.append('/');
    sb.append(tool);
    sb.append('-');
    sb.append(versionString);
    if (this.os != null) {
      sb.append('-');
      sb.append(this.os);
    }
    if (this.arch != null) {
      sb.append('-');
      sb.append(this.arch);
    }
    sb.append(".tgz");
    this.url = sb.toString();
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

    return this.version;
  }

  /**
   * @return {@code true} if {@link OperatingSystem} agnostic, {@code false} otherwise.
   */
  public boolean isOsAgnostic() {

    return this.osAgnostic;
  }

  /**
   * @return {@code true} if {@link SystemArchitecture} agnostic, {@code false} otherwise.
   */
  public boolean isArchAgnostic() {

    return this.archAgnostic;
  }

  /**
   * @return the repository base URL. This may be a typical URL (e.g. "https://host/path") but may also be a path in
   *         your file-system (e.g. to a mounted remote network drive).
   */
  public String getRepositoryUrl() {

    return this.repositoryUrl;
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

  @Override
  public String toString() {

    return "CustomTool[" + this.tool + ":" + this.version + "@" + this.repositoryUrl + "]";
  }

}
