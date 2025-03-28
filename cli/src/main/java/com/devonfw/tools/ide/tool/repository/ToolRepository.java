package com.devonfw.tools.ide.tool.repository;

import java.nio.file.Path;
import java.util.Collection;

import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.AbstractUrlMetadata;
import com.devonfw.tools.ide.url.model.file.json.CVE;
import com.devonfw.tools.ide.url.model.file.json.ToolDependency;
import com.devonfw.tools.ide.url.model.file.json.ToolSecurity;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface for a software repository that allows to {@link #download(String, String, VersionIdentifier, ToolCommandlet) download} software via the network. It
 * is responsible for the following aspects:
 * <ul>
 * <li>version resolution (e.g. resolving version patterns and determining the latest (stable) version)</li>
 * <li>discovery (compute the download URL)</li>
 * <li>checksum verification (ensure the integrity of the downloaded software package to avoid manipulation
 * attacks)</li>
 * </ul>
 */
public interface ToolRepository extends AbstractUrlMetadata {

  /** {@link #getId() ID} of the default {@link ToolRepository} (for ide-urls). */
  String ID_DEFAULT = "default";

  /**
   * @return the repository ID.
   */
  String getId();

  /**
   * @param tool the name of the tool.
   * @param edition the edition of the tool.
   * @param version the {@link VersionIdentifier} to resolve.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the resolved {@link VersionIdentifier}. If the given {@link VersionIdentifier} is NOT a {@link VersionIdentifier#isPattern() pattern} this method
   *     will always just return the given {@link VersionIdentifier}.
   */
  @Override
  VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet);

  /**
   * Will download the requested software specified by the given arguments. If that software is already available in the download-cache it will be returned
   * right away. Otherwise it will be downloaded and put into the download-cache. Additionally the checksum of the file is verified according to the
   * possibilities and strategy of the {@link ToolRepository}.
   *
   * @param tool the name of the tool.
   * @param edition the edition of the tool.
   * @param version the {@link #resolveVersion(String, String, GenericVersionRange, ToolCommandlet) resolved} {@link VersionIdentifier}.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the {@link Path} to the downloaded software package.
   */
  Path download(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet);

  /**
   * @param tool the name of the tool.
   * @param edition the edition of the tool.
   * @param version the {@link #resolveVersion(String, String, GenericVersionRange, ToolCommandlet) resolved} {@link VersionIdentifier}.
   * @return the {@link Collection} of {@link ToolDependency tool dependencies}.
   */
  Collection<ToolDependency> findDependencies(String tool, String edition, VersionIdentifier version);

  /**
   * @param tool the name of the tool.
   * @param edition the edition of the tool.
   * @return the {@link Collection} of {@link CVE cve security}.
   */
  ToolSecurity findSecurity(String tool, String edition);
}
