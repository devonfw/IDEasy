package com.devonfw.tools.ide.tool.uv;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.pip.PypiObject;
import com.devonfw.tools.ide.tool.repository.ArtifactToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link ArtifactToolRepository} for tools installed via {@code uv tool} and resolved from PyPI.
 */
public class UvRepository extends ArtifactToolRepository<UvArtifact, UvArtifactMetadata> {

  /** Base URL of PyPI registry */
  public static final String REGISTRY_URL = "https://pypi.org/pypi/";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  /** {@link #getId() ID} of this repository. */
  public static final String ID = "uv";

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public UvRepository(IdeContext context) {
    super(context);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  protected UvArtifact resolveArtifact(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {
    if (toolCommandlet instanceof UvBasedCommandlet uv) {
      String name = uv.getPackageName();
      if (version == null) {
        version = VersionIdentifier.LATEST;
      }
      return new UvArtifact(name, version.toString());
    }
    throw new UnsupportedOperationException("Tool '" + tool + "' is not supported by uv repository.");
  }

  @Override
  protected List<VersionIdentifier> fetchVersions(UvArtifact artifact) {
    String url = getRegistryUrl() + artifact.getName() + "/json";
    String json = this.context.getFileAccess().download(url);
    try {
      PypiObject pypiObject = MAPPER.readValue(json, PypiObject.class);
      return pypiObject.releases();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to process JSON from " + url, e);
    }
  }

  /**
   * @return the registry URL.
   */
  protected String getRegistryUrl() {
    return REGISTRY_URL;
  }

  @Override
  public UvArtifactMetadata getMetadata(UvArtifact artifact, String tool, String edition) {
    return new UvArtifactMetadata(artifact, tool, edition);
  }
}
