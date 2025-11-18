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

  /** build.gradle file name */
  public static final String BUILD_GRADLE = "build.gradle";

  /** build.gradle.kts file name */
  public static final String BUILD_GRADLE_KTS = "build.gradle.kts";
  private static final String GRADLE_WRAPPER_FILENAME = "gradlew";

  /**
   * The name of the gradle configuration folder.
   */
  public static final String GRADLE_CONFIG_FOLDER = "gradle";


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

  /**
   * @return the {@link Path} to the gradle configuration folder, creates the folder if it was not existing.
   */
  public Path getOrCreateGradleConfFolder() {

    Path confPath = this.context.getConfPath();
    Path gradleConfigFolder = confPath.resolve(GRADLE_CONFIG_FOLDER);
    if (!Files.isDirectory(gradleConfigFolder)) {
      this.context.getFileAccess().mkdirs(gradleConfigFolder);
    }
    return gradleConfigFolder;
  }

}
