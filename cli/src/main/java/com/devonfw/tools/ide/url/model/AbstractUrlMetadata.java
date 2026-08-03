package com.devonfw.tools.ide.url.model;

import java.util.List;

import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Interface providing the API to access tool metadata.
 */
public interface AbstractUrlMetadata {

  /**
   * @param tool the name of the tool.
   * @return the sorted {@link List} of {@link com.devonfw.tools.ide.tool.ToolCommandlet#getConfiguredEdition() editions}.
   */
  List<String> getSortedEditions(String tool);

  /**
   * @param tool the {@link ToolCommandlet#getName() name of the tool}.
   * @param edition the {@link ToolCommandlet#getConfiguredEdition() tool edition}.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the {@link List} of {@link VersionIdentifier}s sorted descending so the latest version comes first and the oldest comes last.
   */
  List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet);

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @param version the {@link GenericVersionRange} to match. May be a {@link VersionIdentifier#isPattern() pattern}, a specific version or {@code null} for
   *     the latest version.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the latest matching {@link VersionIdentifier} for the given {@code tool} and {@code edition}.
   */
  VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet);

}
