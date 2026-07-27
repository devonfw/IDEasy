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
    registry.add("--completion");
    registry.add("--diag");
    registry.add("--file");
    registry.add("-Djava.home=");
    registry.add("-Djdk.java.options=");
    registry.add("-Dmvnd.buildTime=");
    registry.add("--builder");
    registry.add("-Dmvnd.cancelConnectTimeout=");
    registry.add("-Dmvnd.connectTimeout=");
    registry.add("-Dmvnd.coreExtensionsExclude=");
    registry.add("-Dmvnd.daemonStorage=");
    registry.add("-Dmvnd.debug=");
    registry.add("-Dmvnd.debug.address=");
    registry.add("-Dmvnd.duplicateDaemonGracePeriod=");
    registry.add("-Dmvnd.enableAssertions=");
    registry.add("-Dmvnd.expirationCheckDelay=");
    registry.add("-Dmvnd.home=");
    registry.add("-Dmvnd.idleTimeout=");
    registry.add("-Dmvnd.jvmArgs=");
    registry.add("-Dmvnd.keepAlive=");
    registry.add("-Dmvnd.logPurgePeriod=");
    registry.add("-Dmvnd.maxHeapSize=");
    registry.add("-Dmvnd.maxLostKeepAlive=");
    registry.add("-Dmvnd.minHeapSize=");
    registry.add("-Dmvnd.minThreads=");
    registry.add("-Dmvnd.noBuffering=");
    registry.add("-Dmvnd.noDaemon=");
    registry.add("-Dmvnd.noModelCache=");
    registry.add("-Dmvnd.pluginRealmEvictPattern=");
    registry.add("-Dmvnd.propertiesPath=");
    registry.add("--raw-streams");
    registry.add("-Dmvnd.registry=");
    registry.add("-Dmvnd.rollingWindowSize=");
    registry.add("--serial");
  }

  @Override
  public String getToolHelpArguments() {
    return "--help";
  }
}
