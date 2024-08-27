package com.devonfw.tools.ide.tool.plugin;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;

/**
 * Example implementation of {@link PluginBasedCommandlet} for testing.
 */
public class ExamplePluginBasedCommandlet extends PluginBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public ExamplePluginBasedCommandlet(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }
}
