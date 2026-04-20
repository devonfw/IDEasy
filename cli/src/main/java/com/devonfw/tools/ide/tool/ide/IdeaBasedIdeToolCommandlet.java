package com.devonfw.tools.ide.tool.ide;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.process.ProcessResult;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;

/**
 * {@link IdeToolCommandlet} for IDEA based commandlets like: {@link com.devonfw.tools.ide.tool.intellij.Intellij IntelliJ} and
 * {@link com.devonfw.tools.ide.tool.androidstudio.AndroidStudio Android Studio}.
 */
public class IdeaBasedIdeToolCommandlet extends IdeToolCommandlet {

  private static final Logger LOG = LoggerFactory.getLogger(IdeaBasedIdeToolCommandlet.class);

  private static final String VM_OPTIONS_FILE_EXTENSION = ".vmoptions";

  private static final String VM_ARGS_ENV_SUFFIX = "_VM_ARGS";

  private static final String VM_OPTIONS_ENV_SUFFIX = "_VM_OPTIONS";

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
    args.add(plugin.id().replace("+", " "));
    if (customRepo) {
      args.add(plugin.url());
    }
    ProcessResult result = runTool(pc, ProcessMode.DEFAULT, args);
    if (result.isSuccessful()) {
      IdeLogLevel.SUCCESS.log(LOG, "Successfully installed plugin: {}", plugin.name());
      step.success();
      return true;
    } else {
      step.error("Failed to install plugin {} ({}): exit code was {}", plugin.name(), plugin.id(), result.getExitCode());
      return false;
    }
  }

  /**
   * Returns the IDE product prefix used in various files inside the {@code bin} directory, e.g. {@code "idea"} for {@code idea64.exe} or {@code "studio"} for
   * {@code studio64.vmoptions}.
   * <p>
   * By default, this method returns the tool name ({@link #getName()}). Subclasses may override this method if the IDE binary or vmoptions file uses a more
   * specific or different prefix.
   *
   * @return the IDE product prefix
   */
  protected String getIdeProductPrefix() {

    return getName();
  }

  @Override
  public ProcessResult runTool(ProcessContext pc, ProcessMode processMode, List<String> args) {
    args.add(this.context.getWorkspacePath().toString());

    String variableName = getName().toUpperCase(Locale.ROOT).replace("-", "_") + VM_ARGS_ENV_SUFFIX;
    String userVmArgsContent = this.context.getVariables().get(variableName);
    if (userVmArgsContent == null || userVmArgsContent.isEmpty()) {
      return super.runTool(pc, processMode, args);
    }
    String[] userVmArgs = userVmArgsContent.trim().split("\\s+");

    String prefix = getIdeProductPrefix();
    Path defaultVmOptionsPath = resolveDefaultVmOptionsPath(this.getToolPath(), prefix);
    String defaultVmArgsContent = this.context.getFileAccess().readFileContent(defaultVmOptionsPath);
    if (defaultVmArgsContent == null || defaultVmArgsContent.isEmpty()) {
      LOG.debug("Default {} jvm options not found at: {}", getName(), defaultVmOptionsPath);
      return super.runTool(pc, processMode, args);
    }
    String[] defaultVmArgs = defaultVmArgsContent.trim().split("\\s+");

    String userOptionsFileName = "." + prefix + VM_OPTIONS_FILE_EXTENSION;
    Path confPath = this.context.getWorkspacePath().resolve(userOptionsFileName);
    this.context.getFileAccess().writeFileContent(mergeVmArgs(defaultVmArgs, userVmArgs), confPath, true);

    pc.withEnvVar(prefix.toUpperCase() + VM_OPTIONS_ENV_SUFFIX, confPath.toAbsolutePath().toString());
    return super.runTool(pc, processMode, args);
  }

  private Path resolveDefaultVmOptionsPath(Path softwarePath, String ideProductPrefix) {
    if (ideProductPrefix == null) {
      LOG.debug("Binary prefix for tool {} is not set", getName());
      return null;
    }

    if (this.context.getSystemInfo().isWindows()) {
      return softwarePath
          .resolve("bin")
          .resolve(ideProductPrefix + "64.exe" + VM_OPTIONS_FILE_EXTENSION);
    }

    if (this.context.getSystemInfo().isMac()) {
      try {
        return softwarePath.toRealPath()
            .getParent()
            .resolve("bin")
            .resolve(ideProductPrefix + VM_OPTIONS_FILE_EXTENSION);
      } catch (Exception e) {
        LOG.error("Failed to resolve real path for software path: {}", softwarePath, e);
      }
    }

    return softwarePath // Linux
        .resolve("bin")
        .resolve(ideProductPrefix + "64" + VM_OPTIONS_FILE_EXTENSION);
  }

  private String mergeVmArgs(String[] defaults, String[] userArgs) {

    List<String> result = new ArrayList<>(defaults.length + userArgs.length);
    Collections.addAll(result, defaults);
    for (String userArg : userArgs) {
      boolean replaced = false;
      for (int i = 0; i < result.size(); i++) {
        if (isSameJvmKey(result.get(i), userArg)) {
          result.set(i, userArg); //override default arg with user defined arg
          replaced = true;
          break;
        }
      }
      if (!replaced) { // Extend case: user configured arg does not exist in default options
        result.add(userArg);
      }
    }

    return String.join(System.lineSeparator(), result);
  }

  private String extractJvmOptionsKey(String arg) {

    if (arg.startsWith("-Xmx")) {
      return "-Xmx";
    }
    if (arg.startsWith("-Xms")) {
      return "-Xms";
    }
    if (arg.startsWith("-Xmn")) {
      return "-Xmn";
    }
    if (arg.startsWith("-Xss")) {
      return "-Xss";
    }
    if (arg.startsWith("-D")) {
      int eq = arg.indexOf('=');
      return eq > 0 ? arg.substring(0, eq) : arg;
    }
    if (arg.startsWith("-XX:")) {
      String opt = arg.substring(4);
      if (opt.startsWith("+") || opt.startsWith("-")) {
        return "-XX:" + opt.substring(1);
      }
      int eq = opt.indexOf('=');
      return eq > 0 ? "-XX:" + opt.substring(0, eq) : arg;
    }

    return arg;
  }

  private boolean isSameJvmKey(String a, String b) {

    return extractJvmOptionsKey(a).equals(extractJvmOptionsKey(b));
  }
}
