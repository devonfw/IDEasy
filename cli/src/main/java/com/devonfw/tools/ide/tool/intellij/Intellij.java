package com.devonfw.tools.ide.tool.intellij;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.tool.ide.IdeToolCommandlet;
import com.devonfw.tools.ide.tool.ide.IdeaBasedIdeToolCommandlet;
import com.devonfw.tools.ide.tool.java.Java;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link IdeToolCommandlet} for <a href="https://www.jetbrains.com/idea/">IntelliJ</a>.
 */
public class Intellij extends IdeaBasedIdeToolCommandlet {

  private static final String IDEA = "idea";

  private static final String IDEA64_EXE = IDEA + "64.exe";

  private static final String IDEA_BASH_SCRIPT = IDEA + ".sh";

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public Intellij(IdeContext context) {

    super(context, "intellij", Set.of(Tag.INTELLIJ));
  }

  @Override
  protected String getBinaryName() {

    if (this.context.getSystemInfo().isWindows()) {
      return IDEA64_EXE;
    } else {
      if (Files.exists(this.getToolBinPath().resolve(IDEA))) {
        return IDEA;
      } else if (Files.exists(this.getToolBinPath().resolve(IDEA_BASH_SCRIPT))) {
        return IDEA_BASH_SCRIPT;
      } else {
        return IDEA;
      }
    }
  }

  @Override
  protected void setEnvironment(EnvironmentContext environmentContext, ToolInstallation toolInstallation, boolean extraInstallation) {

    super.setEnvironment(environmentContext, toolInstallation, extraInstallation);
    environmentContext.withEnvVar("IDEA_PROPERTIES", this.context.getWorkspacePath().resolve("idea.properties").toString());
  }

  @Override
  protected void installDependencies() {

    // TODO create intellij/intellij/dependencies.json file in ide-urls and delete this method
    // TODO create intellij/ultimate/dependencies.json file in ide-urls and delete this method
    getCommandlet(Java.class).install();
  }

  @Override
  public void installPlugin(ToolPluginDescriptor plugin, Step step) {

    // In case of plugins with a custom repo url
    boolean customRepo = plugin.url() != null;
    List<String> args = new ArrayList<>();
    args.add("installPlugins");
    args.add(plugin.id());
    if (customRepo) {
      args.add(plugin.url());
    }
    ProcessResult result = runTool(ProcessMode.DEFAULT_CAPTURE, null, ProcessErrorHandling.LOG_WARNING, args.toArray(new String[0]));
    if (result.isSuccessful()) {
      for (String line : result.getOut()) {
        if (line.contains("installed plugin: PluginNode{id=" + plugin.id())) {
          step.success();
          return;
        }
      }
    }
    result.log(IdeLogLevel.DEBUG, context, IdeLogLevel.ERROR);
    step.error("Failed to install plugin {} ({}): exit code was {}", plugin.name(), plugin.id(), result.getExitCode());
  }

  @Override
  public void runTool(String... args) {
    List<String> extendedArgs = new ArrayList<>(Arrays.asList(args));
    extendedArgs.add(this.context.getWorkspacePath().toString());
    super.runTool(extendedArgs.toArray(new String[0]));
  }
}
