package com.devonfw.tools.ide.tool.mvn;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.tool.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.ToolCommandlet;
import com.devonfw.tools.ide.tool.ide.PluginDescriptor;
import com.devonfw.tools.ide.tool.java.Java;

/**
 * {@link ToolCommandlet} for <a href="https://maven.apache.org/">maven</a>.
 */
public class Mvn extends PluginBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Mvn(IdeContext context) {

    super(context, "mvn", Set.of(Tag.JAVA, Tag.BUILD));
  }

  @Override
  public boolean install(boolean silent) {

    getCommandlet(Java.class).install();
    return super.install(silent);
  }

  @Override
  protected void postInstall() {

    super.postInstall();
  }

  @Override
  public void installPlugin(PluginDescriptor plugin) {

    Path mavenPlugin = this.context.getSoftwarePath().resolve(this.tool).resolve("lib/ext/" + plugin.getName() + ".jar");
    System.out.println(getPluginUrl(plugin));
    this.context.getFileAccess().download(getPluginUrl(plugin), mavenPlugin);
    if (Files.exists(mavenPlugin)) {
      this.context.success("Successfully added {} to {}", plugin.getName(), mavenPlugin.toString());
    } else {
      this.context.warning("Plugin {} has wrong properties\n" //
          + "Please check the plugin properties file in {}", mavenPlugin.getFileName(), mavenPlugin.toAbsolutePath());
    }
  }

  private String getPluginUrl(PluginDescriptor plugin) {

    if (plugin.getUrl() != null) {
      return plugin.getUrl();
    } else {
      return "https://repo1.maven.org/maven2/fr/jcgay/maven/" + plugin.getName() + "/" + getPluginVersion(plugin.getVersion()) + "/" + plugin.getName()
          + "-" + getPluginVersion(plugin.getVersion()) + ".jar";
    }
  }

  private String getPluginVersion(String version) {

    return Objects.requireNonNullElse(version, "latest");
  }
}
