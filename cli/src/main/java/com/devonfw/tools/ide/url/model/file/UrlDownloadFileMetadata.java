package com.devonfw.tools.ide.url.model.file;

import java.util.Set;

import com.devonfw.tools.ide.common.OperatingSystem;
import com.devonfw.tools.ide.common.SystemArchitecture;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for the metadata of a download file.
 */
public interface UrlDownloadFileMetadata {

  /**
   * @return the name of the tool.
   */
  String getTool();

  /**
   * @return the edition. By default the same as the {@link #getTool() tool}.
   */
  String getEdition();

  /**
   * @return the {@link VersionIdentifier} of the tool.
   */
  VersionIdentifier getVersion();

  /**
   * @return the immutable {@link Set} with the URLs.
   */
  Set<String> getUrls();

  /**
   * @return the dedicated {@link OperatingSystem} or {@code null} if OS-agnostic.
   */
  OperatingSystem getOs();

  /**
   * @return the dedicated {@link SystemArchitecture} or {@code null} if architecture-agnostic.
   */
  SystemArchitecture getArch();

  /**
   * @return the expected checksum of the download package or {@code null} if not available.
   */
  String getChecksum();

}
