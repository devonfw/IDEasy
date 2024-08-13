package com.devonfw.tools.ide.tool.ide;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.plugin.PluginDescriptor;

public class IdeaBasedIdeToolCommandlet extends IdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   * @param tool the {@link #getName() tool name}.
   * @param tags the {@link #getTags() tags} classifying the tool. Should be created via {@link Set#of(Object) Set.of} method.
   */
  public IdeaBasedIdeToolCommandlet(IdeContext context, String tool, Set<Tag> tags) {
    super(context, tool, tags);
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    IdeaBasedPluginInstaller pluginInstaller = new IdeaBasedPluginInstaller(context, this);
    String downloadUrl = pluginInstaller.getDownloadUrl(plugin);
    pluginInstaller.installPlugin(plugin, downloadUrl);
  }
}
