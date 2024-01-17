package com.devonfw.tools.ide.tool;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;

public class PluginBasedCommandletTest extends AbstractIdeContextTest {

  @Test
   void testGetPluginsMap() {
    IdeTestContext context = newContext(PROJECT_BASIC, "", true);
    String tool = "Eclipse";
    Set<Tag> tags = null;
    AbstractPluginBasedCommandletTest pluginBasedCommandlet = new AbstractPluginBasedCommandletTest(context, tool, tags);

    Map<String, PluginDescriptor> pluginsMap = pluginBasedCommandlet.getPluginsMap();
    assertNotNull(pluginsMap);

    assertTrue(pluginsMap.containsKey("checkstyle"));
    assertTrue(pluginsMap.containsKey("anyedit"));

    PluginDescriptor plugin1 = pluginsMap.get("checkstyle");
    assertNotNull(plugin1);
    assertEquals("checkstyle", plugin1.getName());

    PluginDescriptor plugin2 = pluginsMap.get("anyedit");
    assertNotNull(plugin2);
    assertEquals("anyedit", plugin2.getName());

    // Check if anyedit plugin has value "false" --> value from user directory
    assertEquals(false, plugin2.isActive());
  }
}
