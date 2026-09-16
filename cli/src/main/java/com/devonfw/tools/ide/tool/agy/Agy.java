package com.devonfw.tools.ide.tool.agy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallRequest;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link LocalToolCommandlet} for <a href="https://github.com/google-antigravity/antigravity-cli">Antigravity CLI (Agy)</a>.
 */
public class Agy extends LocalToolCommandlet {

  /** Logger. */
  private static final Logger LOG = LoggerFactory.getLogger(Agy.class);

  /** Sub-directory of {@code conf} holding the isolated Agy main configuration. */
  static final String CONFIG_FOLDER = "gemini/antigravity-cli";

  /** Sub-directory of {@code conf} holding the isolated Agy MCP/project configuration. */
  static final String MCP_CONFIG_FOLDER = "gemini/config";

  /**
   * Relative location of the Agy main configuration directory within the user home that Agy reads (settings, theme, conversations). This path is symlinked onto
   * {@link #getAgyConfigDir() the project-local main configuration directory}.
   */
  static final Path HOME_ANTIGRAVITY_CLI_LOCATION = Path.of(".gemini", "antigravity-cli");

  /**
   * Relative location of the Agy MCP/project configuration directory within the user home (MCP servers, project definitions). This path is symlinked onto
   * {@link #getAgyMcpConfigDir() the project-local MCP configuration directory}.
   */
  static final Path HOME_MCP_CONFIG_LOCATION = Path.of(".gemini", "config");

  /**
   * Content of the seeded {@code README.md} explaining how the project-local configuration is isolated and how to declare an API key.
   */
  private static final String README_CONTENT = """
      # Isolated Agy (Antigravity CLI) configuration
      
      Agy has no environment variable to relocate its configuration, so IDEasy keeps it project-isolated by pointing
      Agy's standard home locations at this project:
        - ~/.gemini/antigravity-cli -> $IDE_HOME/conf/gemini/antigravity-cli   (theme, conversations, history)
        - ~/.gemini/config          -> $IDE_HOME/conf/gemini/config            (MCP servers, project definitions)
      The links are (re)created automatically every time you start Agy through IDEasy. The content is owned by you -
      IDEasy only creates the directories and this file, it never modifies your data.
      
      ## Using Agy
      Each IDEasy project has its own Agy binary and its own configuration. Launch Agy through IDEasy (`ide agy`) in
      the project you want to use; the links are re-pointed first, so the right configuration is always picked.
      Note: if you run Agy WITHOUT IDEasy, the links are not re-pointed and Agy uses the configuration of the project
      that most recently set them up.
      
      ## API key
      Put your credentials in settings.json -> env:
        { "env": { "ANTIGRAVITY_API_KEY": "..." } }
      IDEasy removes ANTIGRAVITY_API_KEY when it is only inherited from your ambient system environment, so a leaked
      value cannot override this project's key. A value you declare in an IDEasy `ide.properties` (e.g.
      `settings/ide.properties` to share it with your team) is kept as-is.
      """;

  /**
   * Provider/auth environment variables that must not leak from the ambient system environment into the launched Agy process, so an inherited value cannot
   * override the per-project configuration. Each variable is only removed when it is inherited from the
   * {@link EnvironmentVariablesType#SYSTEM system environment} (or undefined); a value declared in an IDEasy {@code ide.properties} layer (e.g.
   * {@code settings/ide.properties}) is intentional and therefore preserved.
   */
  static final List<String> SCRUB_VARS = List.of("ANTIGRAVITY_API_KEY");

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Agy(IdeContext context) {
    super(context, "agy", Set.of(Tag.ARTIFICIAL_INTELLIGENCE));
  }

  @Override
  public String getToolHelpArguments() {
    return "--help";
  }

  /**
   * @return the name of the installed binary.
   */
  @Override
  protected String getBinaryName() {
    return "antigravity";
  }


  /**
   * @return the {@link Path} to the isolated Agy configuration directory ({@code $IDE_HOME/conf/gemini/antigravity-cli}) or {@code null} if no {@code IDE_HOME}
   *     is present.
   */
  Path getAgyConfigDir() {
    Path confPath = this.context.getConfPath();
    if (confPath == null) {
      return null;
    }
    return confPath.resolve(CONFIG_FOLDER);
  }

  /**
   * @return the {@link Path} to the isolated Agy MCP/project configuration directory ({@code $IDE_HOME/conf/gemini/config}) or {@code null} if no
   *     {@code IDE_HOME} is present.
   */
  Path getAgyMcpConfigDir() {
    Path confPath = this.context.getConfPath();
    if (confPath == null) {
      return null;
    }
    return confPath.resolve(MCP_CONFIG_FOLDER);
  }

  @Override
  public void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean additionalInstallation) {
    super.setEnvironment(environmentContext, toolInstallation, additionalInstallation);
    // Agy has no environment variable to relocate its configuration, so instead of setting an environment variable
    // we (re-)link Agy's standard home location onto the project-local configuration directory.
    linkHomeConfigDir();
    EnvironmentVariables variables = this.context.getVariables();
    for (String name : SCRUB_VARS) {
      if (isInheritedFromSystem(variables, name)) {
        environmentContext.removeEnvVar(name);
      }
    }
  }

  /**
   * Ensures that both Agy home locations resolve to their project-local configuration directories: {@code ~/.gemini/antigravity-cli} onto
   * {@code $IDE_HOME/conf/gemini/antigravity-cli} and {@code ~/.gemini/config} onto {@code $IDE_HOME/conf/gemini/config}.
   */
  void linkHomeConfigDir() {
    if (this.context.getUserHome() == null) {
      return;
    }
    ensureHomeLink(HOME_ANTIGRAVITY_CLI_LOCATION, getAgyConfigDir());
    ensureHomeLink(HOME_MCP_CONFIG_LOCATION, getAgyMcpConfigDir());
  }

  /**
   * Ensures that the given Agy home location resolves to the given project-local target directory by creating or replacing a symbolic link (Windows: a
   * junction). This is a no-op when the home location already resolves to the target. If it is a regular directory that does not resolve to the target, it is
   * left untouched and a warning is logged, so user data is never overwritten.
   *
   * @param homeLocation the Agy home location that Agy reads its configuration from (e.g. {@code .gemini/antigravity-cli}).
   * @param targetDir the project-local configuration directory to link to, or {@code null} to skip.
   */
  private void ensureHomeLink(Path homeLocation, Path targetDir) {
    if (targetDir == null) {
      return;
    }
    Path userHome = this.context.getUserHome();
    // make sure the link target exists so that on Windows a (privilege-free) junction is used instead of an (impossible) hard link to a missing directory
    this.context.getFileAccess().mkdirs(targetDir);
    Path homeConfigPath = userHome.resolve(homeLocation);
    if (Files.isSymbolicLink(homeConfigPath) || this.context.getFileAccess().isJunction(homeConfigPath)) {
      // already a link - only replace it if it does not already resolve to our project-local configuration
      try {
        if (!this.context.getFileAccess().toRealPath(homeConfigPath).equals(this.context.getFileAccess().toRealPath(targetDir))) {
          this.context.getFileAccess().symlink(targetDir, homeConfigPath);
        }
      } catch (RuntimeException e) { // e.g. broken link that cannot be resolved
        this.context.getFileAccess().symlink(targetDir, homeConfigPath);
      }
    } else if (Files.exists(homeConfigPath)) {
      // a regular directory/file that is not a link - do not clobber user data
      LOG.warn(
          "Agy: {} already exists and is not a symbolic link/junction, skipping isolation - "
              + "please move or rename it to use the project-local Agy configuration.",
          homeConfigPath);
    } else {
      // ensure the parent directory exists (link() does not create parents), then create the link
      this.context.getFileAccess().mkdirs(homeConfigPath.getParent());
      this.context.getFileAccess().symlink(targetDir, homeConfigPath);
    }
  }

  /**
   * @param variables the {@link EnvironmentVariables} of the current {@link IdeContext}.
   * @param name the name of the environment variable to check.
   * @return {@code true} if the variable is undefined or only inherited from the {@link EnvironmentVariablesType#SYSTEM system environment} and should
   *     therefore be scrubbed; {@code false} if it is defined in an IDEasy {@code ide.properties} layer and must be preserved so it can be shared
   *     intentionally.
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
   * Creates the isolated config directory with a minimal {@code settings.json} and a {@code README.md} if they do not exist yet. Existing files are never
   * modified - the user owns the content.
   */
  void seedConfigSkeleton() {
    Path configDir = getAgyConfigDir();
    if (configDir == null) {
      return;
    }
    Path settings = configDir.resolve("settings.json");
    if (!Files.exists(settings)) {
      this.context.getFileAccess().writeFileContent("{\n  \"env\": {}\n}\n", settings, true);
    }
    Path readme = configDir.resolve("README.md");
    if (!Files.exists(readme)) {
      this.context.getFileAccess().writeFileContent(README_CONTENT, readme, true);
    }
  }
}
