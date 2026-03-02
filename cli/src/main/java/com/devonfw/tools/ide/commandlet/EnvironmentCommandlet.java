package com.devonfw.tools.ide.commandlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.devonfw.tools.ide.process.EnvironmentContext;
import com.devonfw.tools.ide.process.EnvironmentVariableCollectorContext;
import com.devonfw.tools.ide.property.FlagProperty;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;
import com.devonfw.tools.ide.tool.ToolInstallation;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * {@link Commandlet} to print the environment variables.
 */
public final class EnvironmentCommandlet extends Commandlet {

  /** {@link FlagProperty} to enable Bash (MSys) path conversion on Windows. */
  public final FlagProperty bash;

  /**
   * The constructor.
   *
   * @param context the {@link IdeContext}.
   */
  public EnvironmentCommandlet(IdeContext context) {

    super(context);
    addKeyword(getName());
    this.bash = add(new FlagProperty("--bash"));
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
    Map<String, VariableLine> variableMap = variables.stream().collect(Collectors.toMap(VariableLine::getName, v -> v));

    EnvironmentVariableCollectorContext environmentVariableCollectorContext = new EnvironmentVariableCollectorContext(variableMap,
        new VariableSource(EnvironmentVariablesType.TOOL, this.context.getSoftwarePath()), pathSyntax);
    setEnvironmentVariablesInLocalTools(environmentVariableCollectorContext);

    printLines(variableMap, logger, winCmd);
  }

  private void printLines(Map<String, VariableLine> variableMap, IdeSubLogger logger, boolean winCmd) {
    if (this.context.debug().isEnabled()) {
      Map<EnvironmentVariablesType, List<VariableLine>> type2lines = variableMap.values().stream().collect(Collectors.groupingBy(l -> l.getSource().type()));
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
      List<VariableLine> variables = new ArrayList<>(variableMap.values());
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

  private void setEnvironmentVariablesInLocalTools(EnvironmentContext environmentContext) {
    // installed tools in IDE_HOME/software
    for (Commandlet commandlet : this.context.getCommandletManager().getCommandlets()) {
      if (commandlet instanceof LocalToolCommandlet tool) {
        try {
          if (tool.isInstalled()) {
            // for performance optimization, we do a hack here and assume that the installedVersion is never used by any setEnvironment method implementation.
            VersionIdentifier installedVersion = VersionIdentifier.LATEST;
            ToolInstallation toolInstallation = new ToolInstallation(tool.getToolPath(), tool.getToolPath(),
                tool.getToolBinPath(), installedVersion, false);
            tool.setEnvironment(environmentContext, toolInstallation, false);
          }
        } catch (Exception e) {
          this.context.warning("An error occurred while setting the environment variables in local tools:", e);
        }
      }
    }
  }
}
