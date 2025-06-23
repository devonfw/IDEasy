package com.devonfw.tools.ide.tool.gradle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://gradle.org/">gradle</a>.
 */
public class Gradle extends LocalToolCommandlet {

  private static final String BUILD_GRADLE = "build.gradle";
  private static final String GRADLE_WRAPPER_FILENAME = "gradlew";

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

  @Override
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode, ProcessErrorHandling errorHandling) {
    Path gradle = Path.of(getBinaryName());
    Path wrapper = findWrapper();
    pc.executable(Objects.requireNonNullElse(wrapper, gradle));
  }

  /**
   * Searches for a wrapper file in valid gradle projects (containing a build.gradle) and returns its path.
   *
   * @return Path of the wrapper file or {@code null} if none was found.
   */
  protected Path findWrapper() {
    Path dir = context.getCwd();
    // traverse the cwd containing a build.gradle up till a gradle wrapper file was found
    while (Files.exists(dir.resolve(BUILD_GRADLE)) &&
        !Files.exists(dir.resolve(GRADLE_WRAPPER_FILENAME)) &&
        dir.getParent() != null) {
      dir = dir.getParent();
    }
    if (Files.exists(dir.resolve(GRADLE_WRAPPER_FILENAME))) {
      context.debug("Using gradle wrapper file at: {}", dir);
      return dir.resolve(GRADLE_WRAPPER_FILENAME);
    }

    return null;
  }

}
