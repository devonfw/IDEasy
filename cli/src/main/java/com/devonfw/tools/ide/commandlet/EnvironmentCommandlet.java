package com.devonfw.tools.ide.commandlet;

import java.util.Collection;
import java.util.stream.Collectors;

import com.devonfw.tools.ide.context.AbstractIdeContext;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.environment.VariableLine;
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
    this.bash = add(new FlagProperty("--bash", false, null));
  }

  @Override
  public String getName() {

    return "env";
  }

  @Override
  public boolean isIdeHomeRequired() {

    return true;
  }

  @Override
  public boolean isProcessableOutput() {

    return true;
  }

  @Override
  public void run() {

    WindowsPathSyntax pathSyntax = null;
    if (this.context.getSystemInfo().isWindows()) {
      if (this.bash.isTrue()) {
        pathSyntax = WindowsPathSyntax.MSYS;
      } else {
        pathSyntax = WindowsPathSyntax.WINDOWS;
      }
    }
    ((AbstractIdeContext) this.context).setPathSyntax(pathSyntax);
    Collection<VariableLine> variables = this.context.getVariables().collectVariables();
    if (this.context.debug().isEnabled()) {
      for (String source : variables.stream().map(VariableLine::getSource).collect(Collectors.toSet())) {
        this.context.debug("from {}:", source);
        for (VariableLine line : variables) {
          if (line.getSource().equals(source)) {
            printEnvLine(line);
          }
        }
      }
    } else {
      for (VariableLine line : variables) {
        printEnvLine(line);
      }
    }
  }

  private void printEnvLine(VariableLine line) {
    String lineValue = line.getValue();
    lineValue = "\"" + lineValue + "\"";
    line = line.withValue(lineValue);
    this.context.info(line.toString());
  }
}
