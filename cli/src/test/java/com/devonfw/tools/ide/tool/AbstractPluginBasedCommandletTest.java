package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Set;


public class AbstractPluginBasedCommandletTest extends PluginBasedCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   * method.
   */
  public AbstractPluginBasedCommandletTest(IdeContext context, String tool, Set<String> tags) {

    super(context, tool, tags);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }

  @Test
  public void testGetPluginsMap() {





  }

}
