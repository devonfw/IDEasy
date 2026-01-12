package com.devonfw.tools.ide.tool.npm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.npm.NpmJsonObject;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.node.NodeBasedCommandlet;
import com.devonfw.tools.ide.tool.repository.AbstractToolRepository;
import com.devonfw.tools.ide.tool.repository.ArtifactToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link AbstractToolRepository} for node-based artifacts. Actually {@link com.devonfw.tools.ide.tool.npm.Npm npm} was the first famous
 * package manager for {@link com.devonfw.tools.ide.tool.node.Node node.js}. Meanwhile, there are others like {@link com.devonfw.tools.ide.tool.yarn.Yarn Yarn}.
 * Since the registry is <a href="https://www.npmjs.com/">npmjs.com</a> it is not called node-repository but npm-repository.
 */
public class NpmRepository extends ArtifactToolRepository<NpmArtifact, NpmArtifactMetadata> {

  /** The base URL of the npm registry. */
  public static final String REGISTRY_URL = "https://registry.npmjs.org/";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  /** The {@link #getId() repository ID}. */
  public static final String ID = "npm";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public NpmRepository(IdeContext context) {
    super(context);
  }

  @Override
  public String getId() {

    return ID;
  }

  @Override
  protected NpmArtifact resolveArtifact(String tool, String edition, VersionIdentifier version, ToolCommandlet toolCommandlet) {

    if (toolCommandlet instanceof NodeBasedCommandlet nodeBasedCommandlet) {
      String name = nodeBasedCommandlet.getPackageName();
      if (version == null) {
        version = VersionIdentifier.LATEST;
      }
      return new NpmArtifact(name, version.toString());
    }
    throw new UnsupportedOperationException("Tool '" + tool + "' is not supported by npm repository.");
  }

  @Override
  protected List<VersionIdentifier> fetchVersions(NpmArtifact artifact) {

    String url = getRegistryUrl() + artifact.getName();
    String json = this.context.getFileAccess().download(url);
    try {
      NpmJsonObject npmJsonObject = MAPPER.readValue(json, NpmJsonObject.class);
      Set<String> versionSet = npmJsonObject.versions().getVersionMap().keySet();
      List<VersionIdentifier> versions = new ArrayList<>(versionSet.size());
      for (String version : versionSet) {
        versions.add(VersionIdentifier.of(version));
      }
      return versions;
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
  public NpmArtifactMetadata getMetadata(NpmArtifact artifact, String tool, String edition) {

    return new NpmArtifactMetadata(artifact, tool, edition);
  }
}
