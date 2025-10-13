package com.devonfw.tools.ide.tool.spring;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.MvnArtifact;
import com.devonfw.tools.ide.tool.mvn.MvnBasedLocalToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/spring-projects/spring-boot">Spring-Boot-CLI</a>.
 */
public class Spring extends MvnBasedLocalToolCommandlet {

  /** The {@link MvnArtifact} for Spring. */
  public static final MvnArtifact ARTIFACT = new MvnArtifact("org/springframework/boot", "spring-boot-cli", "*", "tar.gz", "bin");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Spring(IdeContext context) {

    super(context, "spring", ARTIFACT, Set.of(Tag.JAVA, Tag.ARCHITECTURE));
  }
}
