package com.devonfw.tools.ide.tool.rewrite;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

import java.util.Set;

/**
 * {@link ToolCommandlet} for <a href="https://docs.openrewrite.org/">Rewrite</a>.
 */
public class Rewrite extends PluginBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool    the {@link #getName() tool name}.
   * @param tags    the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public Rewrite(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
  }

  /**
   * we do not need to do anything because right now we will only call OpenRewrite in the command line form
   * @param plugin the {@link ToolPluginDescriptor} to install.
   * @param step the {@link Step} for the plugin installation.
   */
  @Override
  public void installPlugin(ToolPluginDescriptor plugin, Step step) {
    //do nothing
  }

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Rewrite(IdeContext context) {

    super(context, "rewrite", Set.of(Tag.JAVA));
  }
}
