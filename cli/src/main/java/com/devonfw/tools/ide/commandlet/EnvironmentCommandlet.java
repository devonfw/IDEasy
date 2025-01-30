package com.devonfw.tools.ide.commandlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.cli.CliExitException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.environment.VariableSource;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.process.EnvironmentVariableCollectorContext;
import com.devonfw.tools.ide.process.ProcessContextImpl;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;

/**
 * {@link Commandlet} to print the environment variables.
 */
public final class EnvironmentCommandlet extends Commandlet {

  /** {@link FlagProperty} to enable Bash (MSys) path conversion on Windows. */
  public final FlagProperty bash;

  private final EnvironmentVariableCollectorContext environmentVariableCollectorContext;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EnvironmentCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.bash = add(new FlagProperty("--bash"));
    this.environmentVariableCollectorContext = new EnvironmentVariableCollectorContext(context);
  }

  @Override
  public String getName() {

    return "env";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return false;
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  @Override
  public void run() {
    if (this.context.getIdeHome() == null) {
      throw new CliExitException();
    }
    boolean winCmd = false;
    WindowsPathSyntax pathSyntax = null;
    IdeSubLogger logger = this.context.level(IdeLogLevel.PROCESSABLE);
    if (this.context.getSystemInfo().isWindows()) {
      if (this.bash.isTrue()) {
        pathSyntax = WindowsPathSyntax.MSYS;
      } else {
        winCmd = true;
        pathSyntax = WindowsPathSyntax.WINDOWS;
      }
    }
    ((AbstractIdeContext) this.context).setPathSyntax(pathSyntax);
    List<VariableLine> variables = this.context.getVariables().collectVariables();

    Map<String, String> systemEnvironmentVariables = this.environmentVariableCollectorContext.setEnvironment();
    Map<EnvironmentVariablesType, List<VariableLine>> systemVariablesToLines = new HashMap<>();
    List<VariableLine> systemVariableList = new ArrayList<>();
    for (Entry<String, String> entry : systemEnvironmentVariables.entrySet()) {
      variables.add(VariableLine.of(false, entry.getKey(), entry.getValue(), new VariableSource(EnvironmentVariablesType.SYSTEM, null)));
//      systemVariablesToLines.put(EnvironmentVariablesType.SYSTEM, List.of(VariableLine.of(true, key, entry)));
    }

    if (this.context.debug().isEnabled()) {
      Map<EnvironmentVariablesType, List<VariableLine>> type2lines = variables.stream().collect(Collectors.groupingBy(l -> l.getSource().type()));
      for (EnvironmentVariablesType type : EnvironmentVariablesType.values()) {
        List<VariableLine> lines = type2lines.get(type);
        if (lines != null) {
          boolean sourcePrinted = false;
          sortVariables(lines);
          for (VariableLine line : lines) {
            if (!sourcePrinted) {
              this.context.debug("from {}:", line.getSource());
              sourcePrinted = true;
            }
            logger.log(format(line, winCmd));
          }
        }
      }
    } else {
      sortVariables(variables);
      for (VariableLine line : variables) {
        logger.log(format(line, winCmd));
      }
    }
  }

  private static void sortVariables(List<VariableLine> lines) {
    lines.sort((c1, c2) -> c1.getName().compareTo(c2.getName()));
  }

  private String format(VariableLine line, boolean winCmd) {

    if (winCmd) {
      return line.getName() + "=" + line.getValue();
    } else {
      String lineValue = line.getValue();
      lineValue = "\"" + lineValue + "\"";
      line = line.withValue(lineValue);
      return line.toString();
    }
  }

  private void setEnvironmentVariablesInLocalTools() {
    // installed tools in IDE_HOME/software
    List<Path> softwarePaths = this.context.getFileAccess().listChildren(this.context.getSoftwarePath(), Files::isDirectory);
    for (Path softwarePath : softwarePaths) {
      String toolName = softwarePath.getFileName().toString();
      LocalToolCommandlet localToolCommandlet = (LocalToolCommandlet) this.context.getCommandletManager().getToolCommandlet(toolName);
      if (localToolCommandlet != null) {
//        this.context.getVariables().set(localToolCommandlet.getName());
        ProcessContextImpl processContext = new ProcessContextImpl(this.context);
        ToolInstallation toolInstallation = new ToolInstallation(localToolCommandlet.getToolPath(), localToolCommandlet.getToolPath(),
            localToolCommandlet.getToolBinPath(), localToolCommandlet.getInstalledVersion(), false);

        localToolCommandlet.setEnvironment(processContext, toolInstallation, false);
        //super.add(new ToolProperty(localToolCommandlet.getName(), true, "tool"));
      }
    }
  }
}
