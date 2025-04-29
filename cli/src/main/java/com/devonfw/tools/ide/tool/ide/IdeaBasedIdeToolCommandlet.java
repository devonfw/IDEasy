package com.devonfw.tools.ide.tool.ide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link IdeToolCommandlet} for IDEA based commandlets like: {@link com.devonfw.tools.ide.tool.intellij.Intellij IntelliJ} and
 * {@link com.devonfw.tools.ide.tool.androidstudio.AndroidStudio Android Studio}.
 */
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
  public boolean installPlugin(ToolPluginDescriptor plugin, final Step step, ProcessContext pc) {

    // In case of plugins with a custom repo url
    boolean customRepo = plugin.url() != null;
    List<String> args = new ArrayList<>();
    args.add("installPlugins");
    args.add(plugin.id());
    if (customRepo) {
      args.add(plugin.url());
    }
    ProcessResult result = runTool(ProcessMode.DEFAULT, ProcessErrorHandling.LOG_WARNING, pc, args.toArray(String[]::new));
    if (result.isSuccessful()) {
      this.context.success("Successfully installed plugin: {}", plugin.name());
      step.success();
      return true;
    } else {
      step.error("Failed to install plugin {} ({}): exit code was {}", plugin.name(), plugin.id(), result.getExitCode());
      return false;
    }
  }

  @Override
  public void runTool(String... args) {
    List<String> extendedArgs = new ArrayList<>(Arrays.asList(args));
    extendedArgs.add(this.context.getWorkspacePath().toString());
    super.runTool(extendedArgs.toArray(new String[0]));
  }
}
