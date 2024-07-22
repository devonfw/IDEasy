package com.devonfw.tools.ide.url.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    UrlTool urlTool = this.repository.getChild(tool);
    if (urlTool == null) {
      throw new CliException("Could not find tool '" + tool + "' in ide-urls metadata!");
    }
    UrlEdition urlEdition = urlTool.getChild(edition);
    if (urlEdition == null) {
      throw new CliException("Could not find edition '" + edition + "' for tool '" + tool + "' in ide-urls metadata!");
    }
    return urlEdition;
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @return the sorted {@link List} of {@link String editions} .
   */
  public List<String> getSortedEditions(String tool) {

    List<String> list = new ArrayList<>();
    UrlTool urlTool = this.repository.getChild(tool);
    if (urlTool == null) {
      this.context.warning("Can't get sorted editions for tool {} because it does not exist in {}.", tool, this.repository.getPath());
    } else {
      for (UrlEdition urlEdition : urlTool.getChildren()) {
        list.add(urlEdition.getName());
      }
    }
    Collections.sort(list);
    return Collections.unmodifiableList(list);
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @return the {@link List} of {@link VersionIdentifier}s sorted descending so the latest version comes first and the oldest comes last.
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
   * @param version the {@link VersionIdentifier} to match. May be a {@link VersionIdentifier#isPattern() pattern}, a specific version or {@code null} for the
   * latest version.
   * @return the latest matching {@link VersionIdentifier} for the given {@code tool} and {@code edition}.
   */
  public UrlVersion getResolvedVersion(String tool, String edition, VersionIdentifier version) {

    if (version == null) {
      version = VersionIdentifier.LATEST;
    }
    UrlEdition urlEdition = getEdition(tool, edition);
    VersionIdentifier resolvedVersion = version;
    if (version.isPattern()) {
      resolvedVersion = resolveVersion(tool, edition, version);
    }
    UrlVersion urlVersion = urlEdition.getChild(resolvedVersion.toString());
    if (urlVersion == null) {
      throw new CliException("Version " + version + " for tool " + tool + " does not exist in edition " + edition + ".");
    }
    return urlVersion;
  }

  private VersionIdentifier resolveVersion(String tool, String edition, VersionIdentifier version) {

    List<VersionIdentifier> versions = getSortedVersions(tool, edition);
    for (VersionIdentifier vi : versions) {
      if (version.matches(vi)) {
        this.context.debug("Resolved version pattern {} to version {}", version, vi);
        return vi;
      }
    }
    // TODO properly consider edition (needs list-versions commandlet enhancement to also support edition)
    throw new CliException(
        "Could not find any version matching '" + version + "' for tool '" + tool + "' - potentially there are " + versions.size() + " version(s) available in "
            + getEdition(tool, edition).getPath() + " but none matched! You can get the list of available versions with the following command:\nide list-versions " + tool);
  }

}
