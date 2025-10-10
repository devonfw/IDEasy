package com.devonfw.tools.ide.tool.spring;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;
import com.devonfw.tools.ide.tool.repository.MvnArtifactMetadata;
import com.devonfw.tools.ide.tool.repository.MvnRepository;
import com.devonfw.tools.ide.tool.repository.ToolRepository;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/spring-projects/spring-boot">Spring-Boot-CLI</a>.
 */
public class Spring extends MvnBasedLocalToolCommandlet {

  /** The {@link MvnArtifact} for Spring. */
  public static final MvnArtifact ARTIFACT = new MvnArtifact("org/springframework/boot", "spring-boot-cli", "*", "tar.gz");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Spring(IdeContext context) {

    super(context, "spring", ARTIFACT, Set.of(Tag.JAVA, Tag.ARCHITECTURE));
  }

  @Override
  protected Path downloadTool(String edition, ToolRepository toolRepository, VersionIdentifier resolvedVersion) {

    MvnRepository mvnRepository = this.context.getMvnRepository();
    MvnArtifact mavenArtifact = ARTIFACT.withVersion(resolvedVersion.toString());
    String fileName = mavenArtifact.getArtifactId() + "-" + mavenArtifact.getVersion() + "-bin." + mavenArtifact.getType();
    MvnArtifact newArtifact = mavenArtifact.withFilename(fileName).withVersion(resolvedVersion.toString());
    MvnArtifactMetadata mavenArtifactMetadata = mvnRepository.getMetadata(newArtifact, this.tool, edition);
    return mvnRepository.download(mavenArtifactMetadata);
  }
}
