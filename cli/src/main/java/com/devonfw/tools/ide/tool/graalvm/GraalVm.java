package com.devonfw.tools.ide.tool.graalvm;

import java.nio.file.Path;
import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.process.ProcessContext;
import com.devonfw.tools.ide.process.ProcessErrorHandling;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.step.Step;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.plugin.PluginBasedCommandlet;
import com.devonfw.tools.ide.tool.plugin.ToolPluginDescriptor;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.graalvm.org/">GraalVM</a>, an advanced JDK with ahead-of-time Native Image compilation.
 */
public class GraalVm extends PluginBasedCommandlet {

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public GraalVm(IdeContext context) {

    super(context, "graalvm", Set.of(Tag.JAVA, Tag.RUNTIME));
  }

  @Override
  public Path getToolPath() {

    return this.context.getSoftwareExtraPath().resolve(getName());
  }

  @Override
  public VersionIdentifier getInstalledVersion() {

    return super.getInstalledVersion(getToolPath());
  }

  @Override
  protected String getBinaryName() {

    return "java";
  }

  @Override
  public void postInstall() {

    EnvironmentVariables envVars = this.context.getVariables().getByType(EnvironmentVariablesType.CONF);
    envVars.set("GRAALVM_HOME", getToolPath().toString(), true);
    envVars.save();
    super.postInstall();
  }

  @Override
  public void installPlugin(ToolPluginDescriptor plugin, Step step) {
    String[] args = { "install", plugin.name() };
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    pc.executable(getToolPath() + "\\bin\\gu");
    configureToolArgs(pc, ProcessMode.DEFAULT, ProcessErrorHandling.THROW_CLI, args);
    pc.run(ProcessMode.DEFAULT);
  }

  @Override
  public void uninstallPlugin(ToolPluginDescriptor plugin) {
    String[] args = { "remove", plugin.name() };
    ProcessContext pc = this.context.newProcess().errorHandling(ProcessErrorHandling.THROW_CLI);
    pc.executable(getToolPath() + "\\bin\\gu");
    configureToolArgs(pc, ProcessMode.DEFAULT, ProcessErrorHandling.THROW_CLI, args);
    pc.run(ProcessMode.DEFAULT);
  }


}
