package com.devonfw.tools.ide.tool.mvnd;


import com.devonfw.tools.ide.completion.AutoCompletionRegistry;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.mvn.MavenCommandlet;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/tools/mvnd.html/">maven daemon</a>.
 */
public class Mvnd extends MavenCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Mvnd(IdeContext context) {

    super(context, "mvnd");
  }

  /**
   * Initializes Maven Daemon-specific auto-completion candidates.
   *
   * @param registry the {@link AutoCompletionRegistry} to initialize.
   */
  @Override
  protected void initAutoCompletionRegistry(AutoCompletionRegistry registry) {

    super.initAutoCompletionRegistry(registry);
    registry.add("--status");
    registry.add("--stop");
    registry.add("--purge");
  }

  @Override
  public String getToolHelpArguments() {
    return "--help";
  }
}
