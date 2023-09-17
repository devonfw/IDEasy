package com.devonfw.tools.ide.repo;

import java.nio.file.Path;

import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for a software repository that allows to {@link #download(String, String, VersionIdentifier) download}
 * software via the network. It is responsible for the following aspects:
 * <ul>
 * <li>version resolution (e.g. resolving version patterns and determining the latest (stable) version)</li>
 * <li>discovery (compute the download URL)</li>
 * <li>checksum verification (ensure the integrity of the downloaded software package to avoid manipulation
 * attacks)</li>
 * </ul>
 */
public interface ToolRepository {

  /** {@link #getId() ID} of the default {@link ToolRepository} (for ide-urls). */
  String ID_DEFAULT = "default";

  /**
   * @return the repository ID.
   */
  String getId();

  /**
   * @param tool the name of the tool to download.
   * @param edition the edition of the tool to download.
   * @param version the {@link VersionIdentifier} to download.
   * @return the resolved {@link VersionIdentifier}. If the given {@link VersionIdentifier} is NOT a
   *         {@link VersionIdentifier#isPattern() pattern} this method will always just return the given
   *         {@link VersionIdentifier}.
   */
  VersionIdentifier resolveVersion(String tool, String edition, VersionIdentifier version);

  /**
   * Will download the requested software specified by the given arguments. If that software is already available in the
   * download-cache it will be returned right away. Otherwise it will be downloaded and put into the download-cache.
   * Additionally the checksum of the file is verified according to the possibilities and strategy of the
   * {@link ToolRepository}.
   *
   * @param tool the name of the tool to download.
   * @param edition the edition of the tool to download.
   * @param version the {@link #resolveVersion(String, String, VersionIdentifier) resolved} {@link VersionIdentifier} to
   *        download.
   * @return the {@link Path} to the downloaded software package.
   */
  Path download(String tool, String edition, VersionIdentifier version);

}
