package com.devonfw.tools.ide.tool.mvn;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.repository.MavenArtifactMetadata;
import com.devonfw.tools.ide.tool.repository.MavenRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Extends {@link LocalToolCommandlet} for {@link Mvn Maven} based tools via {@link com.devonfw.tools.ide.tool.repository.MavenRepository}.
 */
public abstract class MvnBasedLocalToolCommandlet extends LocalToolCommandlet {

  private final MvnArtifact artifact;

  /**
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param artifact the {@link MvnArtifact}.
   * @param tags the {@link #getTags() tags}.
   */
  public MvnBasedLocalToolCommandlet(IdeContext context, String tool, MvnArtifact artifact, Set<Tag> tags) {

    super(context, tool, tags);
    this.artifact = artifact;
  }

  /**
   * @param edition the {@link #getConfiguredEdition() tool edition}. May be ignored.
   * @return the {@link MvnArtifact}.
   */
  public MvnArtifact getArtifact(String edition) {

    assert (edition.equals(this.tool));
    return this.artifact;
  }

  @Override
  public ToolRepository getToolRepository() {

    return this.context.getMavenRepository();
  }

  @Override
  protected Path downloadTool(String edition, ToolRepository toolRepository, VersionIdentifier resolvedVersion) {

    MavenRepository mavenRepository = this.context.getMavenRepository();
    MvnArtifact mavenArtifact = getArtifact(edition);
    MavenArtifactMetadata mavenArtifactMetadata = mavenRepository.getMetadata(mavenArtifact, this.tool, edition);
    return mavenRepository.download(mavenArtifactMetadata);
  }
}
