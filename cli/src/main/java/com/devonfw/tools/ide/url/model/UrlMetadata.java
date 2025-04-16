package com.devonfw.tools.ide.url.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.os.SystemInfo;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.folder.UrlEdition;
import com.devonfw.tools.ide.url.model.folder.UrlRepository;
import com.devonfw.tools.ide.url.model.folder.UrlTool;
import com.devonfw.tools.ide.url.model.folder.UrlVersion;
import com.devonfw.tools.ide.version.GenericVersionRange;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Service to {@link #getEdition(String, String) load} an {@link UrlEdition} to get access to its versions.
 */
public class UrlMetadata implements AbstractUrlMetadata {

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
    return urlTool.getOrCreateChild(edition);
  }

  @Override
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

  @Override
  public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet) {

    String key = tool + "/" + edition;
    return this.toolEdition2VersionMap.computeIfAbsent(key, k -> computeSortedVersions(tool, edition));
  }

  private List<VersionIdentifier> computeSortedVersions(String tool, String edition) {

    List<VersionIdentifier> list = new ArrayList<>();
    UrlEdition urlEdition = getEdition(tool, edition);
    urlEdition.load(false);
    for (UrlVersion urlVersion : urlEdition.getChildren()) {
      VersionIdentifier versionIdentifier = urlVersion.getVersionIdentifier();
      SystemInfo sys = this.context.getSystemInfo();
      try {
        urlVersion.getMatchingUrls(sys.getOs(), sys.getArchitecture());
        list.add(versionIdentifier);
      } catch (IllegalStateException e) {
        // ignore, but do not add versionIdentifier as there is no download available for the current system
      }
    }
    list.sort(Comparator.reverseOrder());
    return Collections.unmodifiableList(list);
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @param version the {@link GenericVersionRange} to match. May be a {@link VersionIdentifier#isPattern() pattern}, a specific version or {@code null} for
   *     the latest version.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the latest matching {@link VersionIdentifier} for the given {@code tool} and {@code edition}.
   */
  @Override
  public VersionIdentifier resolveVersion(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    List<VersionIdentifier> versions = getSortedVersions(tool, edition, toolCommandlet);
    return VersionIdentifier.resolveVersionPattern(version, versions, this.context);
  }

  /**
   * @param tool the name of the {@link UrlTool}.
   * @param edition the name of the {@link UrlEdition}.
   * @param version the {@link GenericVersionRange} to match. May be a {@link VersionIdentifier#isPattern() pattern}, a specific version or {@code null} for
   *     the latest version.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the latest matching {@link UrlVersion} for the given {@code tool} and {@code edition}.
   */
  public UrlVersion getVersionFolder(String tool, String edition, GenericVersionRange version, ToolCommandlet toolCommandlet) {

    VersionIdentifier resolvedVersion = resolveVersion(tool, edition, version, toolCommandlet);
    UrlVersion urlVersion = getEdition(tool, edition).getChild(resolvedVersion.toString());
    if (urlVersion == null) {
      throw new CliException("Version " + version + " for tool " + tool + " does not exist in edition " + edition + ".");
    }
    return urlVersion;
  }

}
