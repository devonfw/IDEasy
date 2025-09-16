package com.devonfw.tools.ide.tool.repository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.devonfw.tools.ide.cache.CachedValue;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.url.model.file.UrlDownloadFileMetadata;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Extends {@link AbstractToolRepository} for artifact repository like {@link MavenRepository} or {@link NpmRepository}.
 */
public abstract class ArtifactToolRepository<A extends SoftwareArtifact, M extends UrlDownloadFileMetadata> extends AbstractToolRepository {

  private final Map<A, CachedValue<List<VersionIdentifier>>> versionCache;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public ArtifactToolRepository(IdeContext context) {

    super(context);
    this.versionCache = new HashMap<>();
  }

  @Override
  public List<String> getSortedEditions(String tool) {

    return List.of(tool);
  }

  @Override
  public List<VersionIdentifier> getSortedVersions(String tool, String edition, ToolCommandlet toolCommandlet) {

    A artifact = resolveArtifact(tool, edition, null, toolCommandlet);
    CachedValue<List<VersionIdentifier>> cachedValue = this.versionCache.computeIfAbsent(artifact,
        a -> new CachedValue<>(() -> computeSortedVersions(artifact)));
    return cachedValue.get();
  }

  private List<VersionIdentifier> computeSortedVersions(A artifact) {

    List<VersionIdentifier> versions = fetchVersions(artifact);
    versions.sort(Comparator.reverseOrder());
    return versions;
  }

  /**
   * @param tool the {@link ToolCommandlet#getName() tool name}.
   * @param edition the {@link ToolCommandlet#getConfiguredEdition()  tool edition}.
   * @param version the {@link SoftwareArtifact#getVersion() tool version} or {@code null} if undefined.
   * @param toolCommandlet the {@link ToolCommandlet}.
   * @return the resolved {@link SoftwareArtifact}.
   */
  protected abstract A resolveArtifact(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet);

  /**
   * @param artifact the {@link SoftwareArtifact}.
   * @return the list of {@link VersionIdentifier}s sorted in descending order.
   */
  protected abstract List<VersionIdentifier> fetchVersions(A artifact);

  @Override
  protected M getMetadata(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    A artifact = resolveArtifact(tool, edition, version, toolCommandlet);
    return getMetadata(artifact, tool, edition);
  }

  /**
   * @param artifact the {@link SoftwareArtifact} to resolve.
   * @param tool the {@link ToolCommandlet#getName() tool name}.
   * @param edition the {@link ToolCommandlet#getConfiguredEdition()  tool edition}.
   * @return the resolved {@link UrlDownloadFileMetadata}.
   */
  public abstract M getMetadata(A artifact, String tool, String edition);

}
