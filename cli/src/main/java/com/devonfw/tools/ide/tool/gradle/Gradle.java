package com.devonfw.tools.ide.tool.gradle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://gradle.org/">gradle</a>.
 */
public class Gradle extends LocalToolCommandlet {

  /**
   * The name of the gradle configuration folder.
   */
  public static final String GRADLE_CONFIG_FOLDER = "gradle";

  /**
   * The name of the legacy gradle configuration folder.
   */
  public static final String GRADLE_CONFIG_LEGACY_FOLDER = ".gradle";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Gradle(IdeContext context) {

    super(context, "gradle", Set.of(Tag.JAVA, Tag.BUILD));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  /**
   * @return the {@link Path} to the gradle configuration folder.
   */
  public Path getGradleConfFolder() {

    Path confPath = this.context.getConfPath();
    Path gradleConfigFolder = confPath.resolve(GRADLE_CONFIG_FOLDER);
    if (!Files.isDirectory(gradleConfigFolder)) {
      this.context.getFileAccess().mkdirs(gradleConfigFolder);
    }
    return gradleConfigFolder;
  }

}
