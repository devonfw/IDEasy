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
  private static final String BUILD_GRADLE_KTS = "build.gradle.kts";
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
    Path wrapper = findWrapper(GRADLE_WRAPPER_FILENAME, path -> Files.exists(path.resolve(BUILD_GRADLE)) || Files.exists(path.resolve(BUILD_GRADLE_KTS)));
    pc.executable(Objects.requireNonNullElse(wrapper, gradle));
  }

}
