package com.devonfw.tools.ide.tool.claude;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

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
}
