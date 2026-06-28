package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
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

  /**
   * Provider/auth environment variables removed from the launched Claude process so an ambient/leaked value cannot override the per-project configuration. The
   * isolated {@code settings.json} env block is the single source of truth.
   */
  private static final String README_CONTENT = """
      # Isolated Claude configuration (managed location, content owned by you)

      This directory is your project-local CLAUDE_CONFIG_DIR. IDEasy points Claude here so this
      project's settings, credentials, MCP servers and history stay separate from other projects.

      Put ALL provider/auth configuration in `settings.json` -> `env`. IDEasy removes the following
      ambient variables before launching Claude, so they must be declared here if you need them:
      ANTHROPIC_*, CLAUDE_CODE_USE_BEDROCK/VERTEX/FOUNDRY, CLAUDE_CODE_OAUTH_TOKEN,
      AWS_PROFILE, AWS_REGION, AWS_*_KEY*, AWS_SESSION_TOKEN, AWS_BEARER_TOKEN_BEDROCK.

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
    for (String name : SCRUB_VARS) {
      environmentContext.removeEnvVar(name);
    }
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
