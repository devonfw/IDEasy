package com.devonfw.tools.ide.tool.gradle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.AbstractLocalToolCommandlet;
import com.devonfw.tools.ide.tool.BuildTool;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link AbstractToolCommandlet} for <a href="https://gradle.org/">gradle</a>.
 */
public class Gradle extends AbstractLocalToolCommandlet implements BuildTool {

  /** build.gradle file name */
  public static final String BUILD_GRADLE = "build.gradle";

  /** build.gradle.kts file name */
  public static final String BUILD_GRADLE_KTS = "build.gradle.kts";
  private static final String GRADLE_WRAPPER_FILENAME = "gradlew";

  /** The gradle.properties file name that holds the project version. */
  public static final String GRADLE_PROPERTIES = "gradle.properties";

  /** The {@link Pattern} of the {@code version} property in {@link #GRADLE_PROPERTIES}. */
  private static final Pattern VERSION_PATTERN = Pattern.compile("^([ \\t]*version[ \\t]*[=:][ \\t]*)(\\S+)", Pattern.MULTILINE);

  /** The default value of the {@code GRADLE_RELEASE_OPTS} variable used to build and deploy a release. */
  private static final String DEFAULT_RELEASE_OPTS = "clean publish";

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
  protected void configureToolBinary(ProcessContext pc, ProcessMode processMode) {
    Path gradle = Path.of(getBinaryName());
    Path wrapper = findWrapper(GRADLE_WRAPPER_FILENAME);
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

  @Override
  public Path findBuildDescriptor(Path directory) {

    Path buildDescriptor = directory.resolve(BUILD_GRADLE);
    if (Files.exists(buildDescriptor)) {
      return buildDescriptor;
    }
    buildDescriptor = directory.resolve(BUILD_GRADLE_KTS);
    if (Files.exists(buildDescriptor)) {
      return buildDescriptor;
    }
    return super.findBuildDescriptor(directory);
  }

  @Override
  public VersionIdentifier getProjectVersion(Path projectPath) {

    Path gradleProperties = getGradleProperties(projectPath);
    Matcher matcher = VERSION_PATTERN.matcher(this.context.getFileAccess().readFileContent(gradleProperties));
    if (!matcher.find()) {
      throw new CliException("Could not find 'version' in " + gradleProperties);
    }
    return VersionIdentifier.of(matcher.group(2));
  }

  @Override
  public void setProjectVersion(Path projectPath, VersionIdentifier version) {

    Path gradleProperties = getGradleProperties(projectPath);
    Matcher matcher = VERSION_PATTERN.matcher(this.context.getFileAccess().readFileContent(gradleProperties));
    if (!matcher.find()) {
      throw new CliException("Could not find 'version' in " + gradleProperties);
    }
    String content = matcher.replaceFirst("$1" + Matcher.quoteReplacement(version.toString()));
    this.context.getFileAccess().writeFileContent(content, gradleProperties);
  }

  @Override
  public ProcessResult buildAndDeploy(List<String> additionalArgs) {

    String options = this.context.getVariables().get("GRADLE_RELEASE_OPTS");
    if ((options == null) || options.isBlank()) {
      options = DEFAULT_RELEASE_OPTS;
    }
    List<String> args = new ArrayList<>(List.of(options.split("\\s+")));
    args.addAll(additionalArgs);
    return runTool(ProcessMode.DEFAULT, null, ProcessErrorHandling.NONE, args);
  }

  private Path getGradleProperties(Path projectPath) {

    Path gradleProperties = projectPath.resolve(GRADLE_PROPERTIES);
    if (!Files.exists(gradleProperties)) {
      throw new CliException("Could not find the gradle properties at " + gradleProperties);
    }
    return gradleProperties;
  }
}
