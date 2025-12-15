package com.devonfw.tools.ide.tool.repository;

import java.util.List;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.pip.Pip;
import com.devonfw.tools.ide.tool.pip.PipArtifact;
import com.devonfw.tools.ide.tool.pip.PipArtifactMetadata;
import com.devonfw.tools.ide.tool.pip.PypiObject;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link AbstractToolRepository} for pip/PyPI artifacts.
 */
public class PipRepository extends ArtifactToolRepository<PipArtifact, PipArtifactMetadata> {

  /** The base URL of the PyPI registry. */
  public static final String REGISTRY_URL = "https://pypi.org/pypi/";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  /** The {@link #getId() repository ID}. */
  public static final String ID = "pip";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public PipRepository(IdeContext context) {
    super(context);
  }

  @Override
  public String getId() {

    return ID;
  }

  @Override
  protected PipArtifact resolveArtifact(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    if (toolCommandlet instanceof Pip pip) {
      String name = pip.getPackageName();
      if (version == null) {
        version = VersionIdentifier.LATEST;
      }
      return new PipArtifact(name, version.toString());
    }
    throw new UnsupportedOperationException("Tool '" + tool + "' is not supported by pip repository.");
  }

  @Override
  protected List<VersionIdentifier> fetchVersions(PipArtifact artifact) {

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
  public String getRegistryUrl() {
    return REGISTRY_URL;
  }

  @Override
  public PipArtifactMetadata getMetadata(PipArtifact artifact, String tool, String edition) {

    return new PipArtifactMetadata(artifact, tool, edition);
  }

  @Override
  public List<String> getSortedEditions(String tool) {

    if ("pip".equals(tool)) {
      return Pip.EDITIONS;
    }
    return super.getSortedEditions(tool);
  }
}
