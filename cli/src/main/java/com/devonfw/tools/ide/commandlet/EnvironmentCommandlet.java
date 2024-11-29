package com.devonfw.tools.ide.commandlet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.cli.CliExitException;
import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.EnvironmentVariablesType;
import com.devonfw.tools.ide.environment.VariableLine;
import com.devonfw.tools.ide.log.IdeLogLevel;
import com.devonfw.tools.ide.log.IdeSubLogger;
import com.devonfw.tools.ide.os.WindowsPathSyntax;
import com.devonfw.tools.ide.property.FlagProperty;

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
    if (context.getIdeHome() == null) {
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
}
