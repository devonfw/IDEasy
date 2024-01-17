package com.devonfw.tools.ide.tool;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;


public class AbstractPluginBasedCommandletTest extends PluginBasedCommandlet {
  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of}
   * method.
   */
  public AbstractPluginBasedCommandletTest(IdeContext context, String tool, Set<Tag> tags) {

    super(context, tool, tags);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }

  @Test
  public void testGetPluginsMap() {





  }

}
