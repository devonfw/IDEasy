package com.devonfw.tools.ide.url.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Service to {@link #getEdition(String, String) load} an {@link UrlEdition} to get access to its versions.
 */
public class UrlMetadata {

  private final IdeContext context;

  private final UrlRepository repository;

  private final Map<String, List<VersionIdentifier>> toolEdition2VersionMap;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public UrlMetadata(IdeContext context) {

    super();
    this.context = context;
    this.repository = new UrlRepository(this.context.getUrlsPath());
    this.toolEdition2VersionMap = new HashMap<>();
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @return the {@link UrlEdition}. Will be lazily loaded.
   */
  public UrlEdition getEdition(String tool, String edition) {

    UrlTool urlTool = this.repository.getOrCreateChild(tool);
    UrlEdition urlEdition = urlTool.getOrCreateChild(edition);
    return urlEdition;
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @return the {@link List} of {@link VersionIdentifier}s sorted descending so the latest version comes first and the
   *         oldest comes last.
   */
  public List<VersionIdentifier> getSortedVersions(String tool, String edition) {

    String key = tool + "/" + edition;
    return this.toolEdition2VersionMap.computeIfAbsent(key, k -> computeSortedVersions(tool, edition));
  }

  private List<VersionIdentifier> computeSortedVersions(String tool, String edition) {

    List<VersionIdentifier> list = new ArrayList<>();
    UrlEdition urlEdition = getEdition(tool, edition);
    urlEdition.load(false);
    for (UrlVersion urlVersion : urlEdition.getChildren()) {
      VersionIdentifier versionIdentifier = urlVersion.getVersionIdentifier();
      list.add(versionIdentifier);
    }
    Collections.sort(list, Comparator.reverseOrder());
    return Collections.unmodifiableList(list);
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @param version the {@link VersionIdentifier} to match. May be a {@link VersionIdentifier#isPattern() pattern}, a
   *        specific version or {@code null} for the latest version.
   * @return the latest matching {@link VersionIdentifier} for the given {@code tool} and {@code edition}.
   */
  public VersionIdentifier getVersion(String tool, String edition, VersionIdentifier version) {

    if (version == null) {
      version = VersionIdentifier.LATEST;
    }
    if (!version.isPattern()) {
      return version;
    }
    List<VersionIdentifier> versions = getSortedVersions(tool, edition);
    for (VersionIdentifier vi : versions) {
      if (vi.matches(version)) {
        this.context.debug("Resolved version pattern {} to version {}", version, vi);
        return vi;
      }
    }
    throw new CliException("Could not find any version matching '" + version + "' for tool '" + tool
        + "' - potentially there are " + versions.size() + " version(s) available in "
        + getEdition(tool, edition).getPath() + " but none matched!");
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @param version the {@link VersionIdentifier} to match. May be a {@link VersionIdentifier#isPattern() pattern}, a
   *        specific version or {@code null} for the latest version.
   * @return the latest matching {@link UrlVersion} for the given {@code tool} and {@code edition}.
   */
  public UrlVersion getVersionFolder(String tool, String edition, VersionIdentifier version) {

    VersionIdentifier resolvedVersion = getVersion(tool, edition, version);
    UrlVersion urlVersion = getEdition(tool, edition).getChild(resolvedVersion.toString());
    Objects.requireNonNull(urlVersion);
    return urlVersion;
  }

}
