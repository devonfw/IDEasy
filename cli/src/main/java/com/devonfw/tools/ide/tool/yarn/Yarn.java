package com.devonfw.tools.ide.tool.yarn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.cli.CliException;
import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.json.JsonMapping;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.tool.BuildTool;
import com.devonfw.tools.ide.tool.npm.NpmBasedCommandlet;
import com.devonfw.tools.ide.version.VersionIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link NpmBasedCommandlet} for <a href="https://yarnpkg.com">yarn</a>.
 */
public class Yarn extends NpmBasedCommandlet implements BuildTool {

  private static final String YARN_LOCK = "yarn.lock";

  private static final ObjectMapper MAPPER = JsonMapping.create();

  /** The default value of the {@code YARN_RELEASE_OPTS} variable used to build and deploy a release. */
  private static final String DEFAULT_RELEASE_OPTS = "publish --non-interactive";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Yarn(IdeContext context) {

    super(context, "yarn", Set.of(Tag.TYPE_SCRIPT, Tag.BUILD));
  }

  @Override
  public Path findBuildDescriptor(Path directory) {

    Path lockFile = directory.resolve(YARN_LOCK);
    if (!Files.exists(lockFile)) {
      return null; // if we do not find a yarn.lock file, we let npm take over the package.json
    }
    return super.findBuildDescriptor(directory);
  }

  @Override
  public VersionIdentifier getProjectVersion(Path projectPath) {

    Path packageJson = projectPath.resolve(PACKAGE_JSON);
    JsonNode version;
    try {
      version = MAPPER.readTree(packageJson.toFile()).get("version");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + packageJson, e);
    }
    if ((version == null) || !version.isTextual()) {
      throw new CliException("Could not find 'version' in " + packageJson);
    }
    return VersionIdentifier.of(version.asText());
  }

  @Override
  public void setProjectVersion(Path projectPath, VersionIdentifier version) {

    runTool(List.of("--cwd", projectPath.toString(), "version", "--new-version", version.toString(), "--no-git-tag-version"));
  }

  @Override
  public ProcessResult buildAndDeploy(List<String> additionalArgs) {

    String options = this.context.getVariables().get("YARN_RELEASE_OPTS");
    if ((options == null) || options.isBlank()) {
      options = DEFAULT_RELEASE_OPTS;
    }
    List<String> args = new ArrayList<>(List.of(options.split("\\s+")));
    args.addAll(additionalArgs);
    return runTool(ProcessMode.DEFAULT, null, ProcessErrorHandling.NONE, args);
  }
}
