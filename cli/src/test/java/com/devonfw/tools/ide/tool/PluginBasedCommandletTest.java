package com.devonfw.tools.ide.tool;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PluginBasedCommandletTest extends AbstractIdeContextTest {

 public IdeTestContext context = newContext(PROJECT_BASIC, "", true);
 public ExamplePluginBasedCommandlet pluginBasedCommandlet = new ExamplePluginBasedCommandlet(context, "eclipse", null);

 @Test
   void testGetPluginsMap() {
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
    assertFalse(plugin2.isActive());

    assertFalse(pluginsMap.containsKey("anyedit2"));

    assertEquals(2, pluginsMap.size());
  }


}
