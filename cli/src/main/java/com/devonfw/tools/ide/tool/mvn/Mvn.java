package com.devonfw.tools.ide.tool.mvn;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/">maven</a>.
 */
public class Mvn extends IdeToolCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Mvn(IdeContext context) {

    super(context, "mvn", Set.of(TAG_JAVA, TAG_BUILD, TAG_IDE));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  protected void postInstall() {

    for (PluginDescriptor plugin : super.getConfiguredPlugins()) {

      if (plugin.isActive()) {
        Path mavenPlugin = this.context.getSoftwarePath().resolve(this.tool)
            .resolve(createPluginPath(plugin.getName()));
        this.context.getFileAccess().download(plugin.getUrl(), mavenPlugin);
        if (Files.exists(mavenPlugin)) {
          this.context.success("Successfully added {} to {}", plugin.getName(), mavenPlugin.toString());
        } else {
          this.context.warning("Plugin " + mavenPlugin.getFileName() + " has wrong properties\n" //
              + "Please check the plugin properties file in");
        }
      }
    }
    super.postInstall();
  }

  private String createPluginPath(String pluginName) {

    return "lib/ext/" + pluginName + ".jar";
  }

  // TODO: Delete this function after abstract PluginBasedToolCommandlet class is implemented
  @Override
  public void installPlugin(PluginDescriptor plugin) {

  }

}