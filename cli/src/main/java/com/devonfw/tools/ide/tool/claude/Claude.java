package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ToolInstallRequest;

/**
 * {@link ToolCommandlet} for <a href="https://github.com/anthropics/claude-code">Claude Code CLI</a>.
 */
public class Claude extends LocalToolCommandlet {

  /** Name of the environment variable that relocates the entire Claude configuration directory. */
  static final String CLAUDE_CONFIG_DIR = "CLAUDE_CONFIG_DIR";

  /** Sub-directory of {@code conf} holding the isolated Claude configuration. */
  static final String CONFIG_FOLDER = "claude";

  /** Content of the seeded {@code README.md} explaining that the user owns {@code settings.json} and which scrubbed variables to declare there. */
  private static final String README_CONTENT = """
      # Isolated Claude configuration (managed location, content owned by you)

      This directory is your project-local CLAUDE_CONFIG_DIR. IDEasy points Claude here so this
      project's settings, credentials, MCP servers and history stay separate from other projects.

      Put provider/auth configuration in `settings.json` -> `env`. Before launching Claude, IDEasy
      removes the following variables when they are only inherited from your ambient shell/system
      environment, so an accidentally leaked value cannot override this project's configuration:
      ANTHROPIC_*, CLAUDE_CODE_USE_BEDROCK/VERTEX/FOUNDRY, CLAUDE_CODE_OAUTH_TOKEN,
      AWS_PROFILE, AWS_REGION, AWS_*_KEY*, AWS_SESSION_TOKEN, AWS_BEARER_TOKEN_BEDROCK.
      A value you declare intentionally in an IDEasy `ide.properties` (e.g. `settings/ide.properties`
      to share `ANTHROPIC_MODEL` or `BEDROCK_MODEL_ID` across your team) is kept as-is.

      Example (AWS Bedrock):
        {
          "model": "us.anthropic.claude-opus-4-8-v1",
          "env": {
            "CLAUDE_CODE_USE_BEDROCK": "1",
            "AWS_PROFILE": "my-project-profile",
            "AWS_REGION": "eu-central-1"
          }
        }

      Example (custom / sovereign endpoint):
        {
          "env": {
            "ANTHROPIC_BASE_URL": "https://your-endpoint.example",
            "ANTHROPIC_AUTH_TOKEN": "..."
          }
        }
      """;

  /**
   * Provider/auth environment variables that must not leak from the ambient system environment into the launched Claude process, so an inherited value cannot
   * override the per-project configuration. Each variable is only removed when it is inherited from the {@link EnvironmentVariablesType#SYSTEM system
   * environment} (or undefined); a value declared in an IDEasy {@code ide.properties} layer (e.g. {@code settings/ide.properties} to share
   * {@code ANTHROPIC_MODEL} across a team) is intentional and therefore preserved.
   */
  static final List<String> SCRUB_VARS = List.of(
      "ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_BASE_URL",
      "ANTHROPIC_MODEL", "ANTHROPIC_SMALL_FAST_MODEL", "ANTHROPIC_CUSTOM_HEADERS",
      "ANTHROPIC_DEFAULT_OPUS_MODEL", "ANTHROPIC_DEFAULT_SONNET_MODEL", "ANTHROPIC_DEFAULT_HAIKU_MODEL",
      "ANTHROPIC_BEDROCK_BASE_URL",
      "CLAUDE_CODE_USE_BEDROCK", "CLAUDE_CODE_USE_VERTEX", "CLAUDE_CODE_USE_FOUNDRY",
      "CLAUDE_CODE_OAUTH_TOKEN",
      "AWS_PROFILE", "AWS_REGION", "AWS_DEFAULT_REGION",
      "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_SESSION_TOKEN",
      "AWS_BEARER_TOKEN_BEDROCK");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Claude(IdeContext context) {

    super(context, "claude", Set.of(Tag.ARTIFICIAL_INTELLIGENCE));
  }

  @Override
  public String getToolHelpArguments() {

    return "--help";
  }

  /**
   * @return the {@link Path} to the isolated Claude configuration directory ({@code $IDE_HOME/conf/claude}) or {@code null} if no {@code IDE_HOME} is present.
   */
  Path getClaudeConfigDir() {

    Path confPath = this.context.getConfPath();
    if (confPath == null) {
      return null;
    }
    return confPath.resolve(CONFIG_FOLDER);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    Path claudeConfigDir = getClaudeConfigDir();
    if (claudeConfigDir == null) {
      return;
    }
    environmentContext.withEnvVar(CLAUDE_CONFIG_DIR, claudeConfigDir.toString());
    EnvironmentVariables variables = this.context.getVariables();
    for (String name : SCRUB_VARS) {
      if (isInheritedFromSystem(variables, name)) {
        environmentContext.removeEnvVar(name);
      }
    }
  }

  /**
   * @param variables the {@link EnvironmentVariables} of the current {@link IdeContext}.
   * @param name the name of the environment variable to check.
   * @return {@code true} if the variable is undefined or only inherited from the {@link EnvironmentVariablesType#SYSTEM system environment} and should therefore
   *     be scrubbed; {@code false} if it is defined in an IDEasy {@code ide.properties} layer and must be preserved so it can be shared intentionally (e.g. via
   *     {@code settings/ide.properties}).
   */
  private static boolean isInheritedFromSystem(EnvironmentVariables variables, String name) {

    EnvironmentVariables source = variables.findVariable(name);
    return (source == null) || (source.getType() == EnvironmentVariablesType.SYSTEM);
  }

  @Override
  protected void postInstall(ToolInstallRequest request) {

    super.postInstall(request);
    seedConfigSkeleton();
  }

  /**
   * Creates the isolated config directory with a minimal valid {@code settings.json} and a {@code README.md} if they do not exist yet. Existing files are never
   * modified - the user owns the content.
   */
  private void seedConfigSkeleton() {

    Path claudeConfigDir = getClaudeConfigDir();
    if (claudeConfigDir == null) {
      return;
    }
    Path settings = claudeConfigDir.resolve("settings.json");
    if (!Files.exists(settings)) {
      this.context.getFileAccess().writeFileContent("{\n  \"env\": {}\n}\n", settings, true);
    }
    Path readme = claudeConfigDir.resolve("README.md");
    if (!Files.exists(readme)) {
      this.context.getFileAccess().writeFileContent(README_CONTENT, readme, true);
    }
  }
}
