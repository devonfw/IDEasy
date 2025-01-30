package com.devonfw.tools.ide.process;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariables;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.property.Property;

/**
 * Implementation of {@link EnvironmentContext}. This class collects environment variables of all installed local tools.
 */
public class EnvironmentVariableCollectorContext implements EnvironmentContext {

  /** The owning {@link IdeContext}. */
  protected final IdeContext context;

  private final List<Path> extraPathEntries;

  private Map<String, Property> environmentVariables = new HashMap<>();

  private ProcessErrorHandling errorHandling;

  /**
   * The constructor.
   *
   * @param context the owning {@link IdeContext}.
   */
  public EnvironmentVariableCollectorContext(IdeContext context) {
    super();
    this.context = context;
    this.extraPathEntries = new ArrayList<>();
  }

  @Override
  public EnvironmentContext withEnvVar(String key, String value) {
    return null;
  }

  @Override
  public EnvironmentContext withPathEntry(Path path) {

    this.extraPathEntries.add(path);
    return this;
  }

  public Map<String, String> setEnvironment() {
    Map<String, String> toolVariables = new HashMap<>();
    toolVariables.putAll(this.context.getSystem().getEnv());
    EnvironmentVariables environmentVariables = this.context.getVariables();
    Set<VariableLine> variables = new HashSet<>();

    while (environmentVariables != null) {
      if (!environmentVariables.getType().equals(EnvironmentVariablesType.SYSTEM)) {
        variables.addAll(environmentVariables.collectVariables());
      }
      environmentVariables = environmentVariables.getParent();
    }

    for (VariableLine variable : variables) {
      toolVariables.remove(variable.getName());
    }

//    List<Path> softwarePaths = this.context.getFileAccess().listChildren(this.context.getSoftwarePath(), Files::isDirectory);
//    for (Path softwarePath : softwarePaths) {
//      String toolName = softwarePath.getFileName().toString();
//      LocalToolCommandlet localToolCommandlet = (LocalToolCommandlet) this.context.getCommandletManager().getToolCommandlet(toolName);
//      if (localToolCommandlet != null) {
////        this.context.getVariables().set(localToolCommandlet.getName());
//        ProcessContextImpl processContext = new ProcessContextImpl(this.context);
//        ToolInstallation toolInstallation = new ToolInstallation(localToolCommandlet.getToolPath(), localToolCommandlet.getToolPath(),
//            localToolCommandlet.getToolBinPath(), localToolCommandlet.getInstalledVersion(), false);
//
//        localToolCommandlet.setEnvironment(processContext, toolInstallation, false);
//        //super.add(new ToolProperty(localToolCommandlet.getName(), true, "tool"));
//      }
//    }
    return toolVariables;
  }
}
